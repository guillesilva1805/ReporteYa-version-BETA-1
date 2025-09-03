# Smoke test: Supabase Auth + RLS + n8n (JWT)

Este smoke test valida, sin tocar la app, que:
- Registro y login con Supabase Auth (alias DNI@interna.tuapp) funcionan
- Se obtiene JWT y se puede consultar RLS de `verified_employees`
- Sin JWT, los endpoints protegidos fallan (401/403)
- Con JWT, sólo se ve la propia fila o se reporta rol admin
- Intento de UPDATE como usuario normal falla (403 esperado)
- El repo no contiene llamadas a n8n que envíen contraseñas

## Requisitos
- Python 3.9+

## Variables de entorno
Crea un fichero `.env` (puedes copiar de `.env.example`) o exporta en tu shell:

- `SUPABASE_URL` (p.ej. https://uppdkjfjxtjnukftgwhz.supabase.co)
- `SUPABASE_ANON_KEY` (anon key del proyecto)
- `TEST_PASSWORD` (opcional; por defecto genera una contraseña segura aleatoria)
- `REPO_ROOT` (opcional; por defecto `..` para escanear el repo actual)

## Cómo correr
```bash
cd smoketest
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
# (opcional) export SUPABASE_URL=... SUPABASE_ANON_KEY=...
python test_supabase_auth.py
```

## Salida
- Se genera `report.md` con PASS/FAIL detallado y logs mínimos.

## Nota
- El script no usa service_role.
- No modifica archivos de la app; sólo lee para el escaneo de patrones.
