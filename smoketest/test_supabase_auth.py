import os
import re
import json
import time
import random
import string
from typing import Tuple

import requests
from dotenv import load_dotenv

load_dotenv()

SUPABASE_URL = os.environ.get("SUPABASE_URL", "")
SUPABASE_ANON_KEY = os.environ.get("SUPABASE_ANON_KEY", "")
TEST_PASSWORD = os.environ.get("TEST_PASSWORD")
REPO_ROOT = os.environ.get("REPO_ROOT", "..")

alias = lambda dni: f"{dni}@interna.tuapp"
headers = {"apikey": SUPABASE_ANON_KEY, "Content-Type": "application/json"}

REPORT_LINES = []


def log(line: str):
    print(line)
    REPORT_LINES.append(line)


def rand_dni() -> str:
    return "7" + "".join(random.choice(string.digits) for _ in range(7))


def ensure_env():
    missing = []
    if not SUPABASE_URL:
        missing.append("SUPABASE_URL")
    if not SUPABASE_ANON_KEY:
        missing.append("SUPABASE_ANON_KEY")
    if missing:
        raise SystemExit(f"Faltan variables de entorno: {', '.join(missing)}")


def signup(dni: str, password: str) -> Tuple[bool, str]:
    url = f"{SUPABASE_URL}/auth/v1/signup"
    body = {"email": alias(dni), "password": password}
    r = requests.post(url, headers=headers, data=json.dumps(body), timeout=20)
    if r.status_code // 100 == 2:
        return True, "OK"
    try:
        msg = r.json().get("msg") or r.json().get("error_description") or r.text
    except Exception:
        msg = r.text
    return False, msg


def signin(dni: str, password: str) -> Tuple[bool, str, str]:
    url = f"{SUPABASE_URL}/auth/v1/token?grant_type=password"
    body = {"email": alias(dni), "password": password}
    r = requests.post(url, headers=headers, data=json.dumps(body), timeout=20)
    if r.status_code // 100 == 2:
        data = r.json()
        return True, data.get("access_token", ""), data.get("refresh_token", "")
    try:
        msg = r.json().get("msg") or r.json().get("error_description") or r.text
    except Exception:
        msg = r.text
    return False, "", msg


def get_user(jwt: str) -> Tuple[bool, int]:
    url = f"{SUPABASE_URL}/auth/v1/user"
    r = requests.get(url, headers={**headers, "Authorization": f"Bearer {jwt}"}, timeout=20)
    return r.status_code // 100 == 2, r.status_code


def query_verified_employees(jwt: str | None) -> requests.Response:
    url = f"{SUPABASE_URL}/rest/v1/verified_employees?select=*"
    hdrs = {"apikey": SUPABASE_ANON_KEY}
    if jwt:
        hdrs["Authorization"] = f"Bearer {jwt}"
    return requests.get(url, headers=hdrs, timeout=20)


def try_update_verified_employees(jwt: str | None) -> int:
    url = f"{SUPABASE_URL}/rest/v1/verified_employees?id=eq.__self_test__"
    hdrs = {"apikey": SUPABASE_ANON_KEY, "Content-Type": "application/json"}
    if jwt:
        hdrs["Authorization"] = f"Bearer {jwt}"
    return requests.patch(url, headers=hdrs, data=json.dumps({"note": "should_fail"}), timeout=20).status_code


def scan_repo_for_password_to_n8n() -> list:
    # Busca en código llamadas a n8n con campos password/pwd/pass
    paths = []
    pattern = re.compile(r"https?://[^\s]*n8n[^\s]*", re.I)
    for root, _, files in os.walk(REPO_ROOT):
        for f in files:
            if f.endswith((".kt", ".kts", ".md", ".json")):
                p = os.path.join(root, f)
                try:
                    txt = open(p, "r", encoding="utf-8", errors="ignore").read()
                except Exception:
                    continue
                if pattern.search(txt) and re.search(r"\b(pass|pwd|password|contraseñ)\b", txt, re.I):
                    paths.append(p)
    return paths


def main():
    ensure_env()
    log("# Smoke Test Supabase Auth + RLS\n")

    dni = rand_dni()
    password = TEST_PASSWORD or ("A" + "".join(random.choice(string.ascii_letters + string.digits) for _ in range(10)) + "1")

    log(f"DNI de prueba: `{dni}`")

    ok, msg = signup(dni, password)
    log(f"- Sign up: {'PASS' if ok else 'FAIL'} ({msg if not ok else 'created or pending confirmation'})")

    ok, jwt, err = signin(dni, password)
    log(f"- Sign in: {'PASS' if ok and jwt else 'FAIL'}" + (f" (JWT len={len(jwt)})" if jwt else f" ({err})"))

    if not jwt:
        open("report.md", "w", encoding="utf-8").write("\n".join(REPORT_LINES))
        raise SystemExit(1)

    ok_user, code_user = get_user(jwt)
    log(f"- WhoAmI con JWT: {'PASS' if ok_user else 'FAIL'} (code {code_user})")

    r_no = query_verified_employees(None)
    log(f"- GET verified_employees sin JWT: {'PASS' if r_no.status_code in (401,403) else 'FAIL'} (code {r_no.status_code})")

    r_yes = query_verified_employees(jwt)
    only_self = r_yes.status_code // 100 == 2 and isinstance(r_yes.json(), list)
    size = len(r_yes.json()) if only_self else 0
    log(f"- GET verified_employees con JWT: {'PASS' if only_self else 'FAIL'} (items={size}, code {r_yes.status_code})")

    code_upd = try_update_verified_employees(jwt)
    log(f"- UPDATE como usuario normal (esperado 403): {'PASS' if code_upd in (401,403) else 'FAIL'} (code {code_upd})")

    leaks = scan_repo_for_password_to_n8n()
    if leaks:
        log("- Escaneo n8n con contraseñas: FAIL")
        for p in leaks:
            log(f"  * {p}")
    else:
        log("- Escaneo n8n con contraseñas: PASS")

    open("smoketest/report.md", "w", encoding="utf-8").write("\n".join(REPORT_LINES))
    log("\nReporte escrito en smoketest/report.md")


if __name__ == "__main__":
    main()
