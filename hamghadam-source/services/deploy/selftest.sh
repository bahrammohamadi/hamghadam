#!/usr/bin/env bash
# آزمون end-to-end اسکریپت استقرار، روی سرور جعلی — بدون شبکه، بدون راز واقعی.
#   bash hamghadam-source/services/deploy/selftest.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FAILURES=0
pass() { printf '  \033[32m✔\033[0m %s\n' "$*"; }
fail() { printf '  \033[31m✖ %s\033[0m\n' "$*"; FAILURES=$((FAILURES + 1)); }

check() { # check <توضیح> <شرط-صفر/یک>
    if [ "$2" = "0" ]; then pass "$1"; else fail "$1"; fi
}

run_case() { # run_case <برچسب> <FORCE_SPLIT 0|1>
    local label="$1" force="$2" port out rc state

    FORCE_SPLIT="$force" python3 "$HERE/mock_server.py" 0 > "$HERE/.mock-port" 2>"$HERE/.mock-err" &
    local mock_pid=$!
    for _ in $(seq 1 40); do
        [ -s "$HERE/.mock-port" ] && break
        sleep 0.1
    done
    port="$(head -1 "$HERE/.mock-port")"
    if [ -z "$port" ]; then
        fail "$label: سرور جعلی بالا نیامد"
        kill "$mock_pid" 2>/dev/null
        return 1
    fi

    printf '\n\033[1m— %s —\033[0m\n' "$label"
    out="$(env -i PATH="$PATH" HOME="$HOME" \
        SUPABASE_API="http://127.0.0.1:$port" \
        VERCEL_API="http://127.0.0.1:$port" \
        HEALTH_BASE="http://127.0.0.1:$port" \
        SUPABASE_PAT='sbp_FAKE_pat_for_tests_0001' \
        SUPABASE_PROJECT_REF='testref' \
        SUPABASE_SERVICE_ROLE_KEY='service-role-FAKE-value' \
        VERCEL_TOKEN='vcp_FAKE_vercel_token_0002' \
        AI_API_KEY='sk-FAKE-ai-key-0003' \
        AI_MODEL='test-model' \
        bash "$HERE/deploy.sh" 2>&1)"
    rc=$?

    state="$(curl -s "http://127.0.0.1:$port/__state")"
    kill "$mock_pid" 2>/dev/null
    wait "$mock_pid" 2>/dev/null
    rm -f "$HERE/.mock-port" "$HERE/.mock-err"

    # ---- assertions
    check "$label: اسکریپت با کد ۰ تمام شد" "$([ "$rc" = "0" ] && echo 0 || echo 1)"
    if [ "$rc" != "0" ]; then
        printf '%s\n' "$out" | tail -20 | sed 's/^/      /'
        printf '%s\n' "$state" > "$HERE/.state-failed.json"
        return 1
    fi

    printf '%s' "$out" | grep -q 'sbp_FAKE_pat_for_tests_0001' && fail "$label: توکن سوپابیس در خروجی لو رفت" || pass "$label: توکن سوپابیس در خروجی نیست"
    printf '%s' "$out" | grep -q 'vcp_FAKE_vercel_token_0002' && fail "$label: توکن ورسل در خروجی لو رفت" || pass "$label: توکن ورسل در خروجی نیست"
    printf '%s' "$out" | grep -q 'sk-FAKE-ai-key-0003' && fail "$label: کلید AI در خروجی لو رفت" || pass "$label: کلید AI در خروجی نیست"
    printf '%s' "$out" | grep -q 'service-role-FAKE-value' && fail "$label: کلید سرویس‌رول در خروجی لو رفت" || pass "$label: کلید سرویس‌رول در خروجی نیست"

    local problems
    problems="$(python3 - "$state" "$label" <<'PY'
import json, sys
problems = []
state = json.loads(sys.argv[1])
queries = state['queries']
joined = '\n'.join(queries)
for table in ('public.tasks', 'public.profiles', 'public.devices', 'public.agent_runs'):
    if f'create table if not exists {table}' not in joined:
        problems.append(f'جدول {table} در SQL نبود')
if 'enable row level security' not in joined:
    problems.append('RLS فعال نشد')
if state['envs'].get('AI_API_KEY') != 'sk-FAKE-ai-key-0003':
    problems.append('کلید AI روی ورسل ثبت نشد')
for f in ('api/health.js', 'api/agent.js', 'lib/agent.js'):
    if f not in state['deployment_files']:
        problems.append(f'{f} در بستهٔ استقرار نبود')
for problem in problems:
    print(problem)
PY
)"
    if [ -z "$problems" ]; then
        pass "$label: محتوای SQL، متغیرها و بستهٔ استقرار درست است"
    else
        local line
        while IFS= read -r line; do
            [ -n "$line" ] && fail "$label: $line"
        done <<< "$problems"
    fi

    local statements
    statements="$(printf '%s' "$state" | python3 -c 'import json,sys;print(len(json.load(sys.stdin)["queries"]))')"
    if [ "$force" = "1" ]; then
        check "$label: مسیر دستور‌به‌دستور فعال شد ($statements دستور)" \
              "$([ "$statements" -gt 10 ] && echo 0 || echo 1)"
    else
        check "$label: کل اسکیما در یک درخواست رفت" \
              "$([ "$statements" = "1" ] && echo 0 || echo 1)"
    fi
    return 0
}

printf '\033[1mآزمون اسکریپت استقرار\033[0m\n'
run_case "مسیر عادی" 0
run_case "مسیر جایگزین (SQL دستور‌به‌دستور)" 1

printf '\n'
if [ "$FAILURES" = "0" ]; then
    printf '\033[32mهمهٔ بررسی‌ها سبز\033[0m\n'
    exit 0
fi
printf '\033[31m%d بررسی شکست خورد\033[0m\n' "$FAILURES"
exit 1
