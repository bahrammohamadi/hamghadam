#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""کمک‌کنندهٔ JSON/SQL برای deploy.sh — بدون هیچ وابستگی بیرونی.

زیردستورها (خروجی همیشه یک مقدار ساده یا JSON به stdout):
  statements <file.sql>        تقسیم SQL به دستورهای مستقل (با احترام به $$ و رشته‌ها)
  querybody <file.sql>         {"query": "..."} برای Management API سوپابیس
  apikey <json>                کلید anon/publishable از پاسخ /api-keys
  project <json> <ref>         «id<TAB>name» اگر ref در فهرست پروژه‌ها بود
  envid <json> <key>           شناسهٔ متغیر محیطی با نام داده‌شده
  deploybody <dir> <project>   بدنهٔ POST /v13/deployments با فایل‌های inline
  field <json> <name>          .name از پاسخ JSON
  error <json>                 پیام خطای سرور (برای گزارش)
"""
import json
import os
import sys

TEXT_EXTS = {'.js', '.mjs', '.json', '.txt', '.md', '.yaml', '.yml'}
SKIP_DIRS = {'node_modules', 'test', '.git', '.vercel', '__tests__'}
SKIP_FILES = {'.env', '.env.local', '.env.example', 'package-lock.json'}
MAX_TOTAL_BYTES = 3 * 1024 * 1024


def die(message, code=2):
    sys.stderr.write(f'{message}\n')
    sys.exit(code)


def load_json(path):
    try:
        with open(path, encoding='utf-8') as handle:
            return json.load(handle)
    except FileNotFoundError:
        die(f'فایل پاسخ پیدا نشد: {path}')
    except Exception as exc:  # noqa: BLE001 - هر پاسخ غیر JSON یعنی خطای سرور
        die(f'پاسخ JSON نامعتبر: {exc}')


# --------------------------------------------------------------------- SQL
def clean(text):
    """حذف خطوط خالی و فاصله‌های اضافی ناشی از برداشتن کامنت‌ها (نه داخل رشته‌ها)."""
    lines = [line.rstrip() for line in text.splitlines()]
    return '\n'.join(line for line in lines if line.strip())


def split_statements(sql):
    """تقسیم با آگاهی از رشته، کامنت و بدنهٔ $$ ... $$ (تابع plpgsql)."""
    out, buf = [], []
    i, n = 0, len(sql)
    in_single = False
    dollar = None
    in_line_comment = False
    in_block_comment = False

    while i < n:
        ch = sql[i]
        nxt = sql[i + 1] if i + 1 < n else ''

        if in_line_comment:
            if ch == '\n':
                in_line_comment = False
                buf.append('\n')
            i += 1
            continue
        if in_block_comment:
            if ch == '*' and nxt == '/':
                in_block_comment = False
                buf.append(' ')
                i += 2
                continue
            i += 1
            continue
        if in_single:
            buf.append(ch)
            if ch == "'":
                if nxt == "'":  # '' = نقل‌قول فرار
                    buf.append(nxt)
                    i += 2
                    continue
                in_single = False
            i += 1
            continue
        if dollar is not None:
            if sql.startswith(dollar, i):
                buf.append(dollar)
                i += len(dollar)
                dollar = None
                continue
            buf.append(ch)
            i += 1
            continue

        if ch == '-' and nxt == '-':
            in_line_comment = True
            i += 2
            continue
        if ch == '/' and nxt == '*':
            in_block_comment = True
            i += 2
            continue
        if ch == "'":
            in_single = True
            buf.append(ch)
            i += 1
            continue
        if ch == '$':
            j = i + 1
            while j < n and (sql[j].isalnum() or sql[j] == '_'):
                j += 1
            if j < n and sql[j] == '$':
                dollar = sql[i:j + 1]
                buf.append(dollar)
                i = j + 1
                continue
        if ch == ';':
            statement = clean(''.join(buf))
            buf = []
            if statement and statement.lower() not in ('begin', 'commit', 'rollback'):
                out.append(statement)
            i += 1
            continue

        buf.append(ch)
        i += 1

    tail = clean(''.join(buf))
    if tail and tail.lower() not in ('begin', 'commit', 'rollback'):
        out.append(tail)
    return out


# -------------------------------------------------------------------- files
def collect_files(root):
    files = []
    total = 0
    for base, dirs, names in os.walk(root):
        dirs[:] = sorted(d for d in dirs if d not in SKIP_DIRS)
        for name in sorted(names):
            if name in SKIP_FILES or name.startswith('.'):
                continue
            path = os.path.join(base, name)
            if os.path.splitext(name)[1].lower() not in TEXT_EXTS:
                continue
            relative = os.path.relpath(path, root).replace(os.sep, '/')
            with open(path, encoding='utf-8') as handle:
                data = handle.read()
            total += len(data.encode('utf-8'))
            files.append({'file': relative, 'data': data})
    if total > MAX_TOTAL_BYTES:
        die(f'حجم فایل‌های استقرار بیش از حد است: {total} بایت')
    return files


# --------------------------------------------------------------------- main
def main(argv):
    if len(argv) < 2:
        die(__doc__)
    command = argv[1]

    if command == 'statements':
        for statement in split_statements(open(argv[2], encoding='utf-8').read()):
            sys.stdout.write(json.dumps(statement, ensure_ascii=False) + '\n')
        return 0

    if command == 'querybody':
        sql = open(argv[2], encoding='utf-8').read()
        sys.stdout.write(json.dumps({'query': sql}, ensure_ascii=False))
        return 0

    if command == 'apikey':
        keys = load_json(argv[2])
        items = keys if isinstance(keys, list) else keys.get('api_keys', [])
        for entry in items:
            name = str(entry.get('name') or entry.get('type') or '').lower()
            if name in ('anon', 'publishable', 'anon_key') and entry.get('api_key'):
                sys.stdout.write(entry['api_key'])
                return 0
        die('کلید anon در پاسخ سوپابیس نبود')

    if command == 'project':
        ref = argv[3]
        projects = load_json(argv[2])
        items = projects if isinstance(projects, list) else projects.get('projects', [])
        for entry in items:
            if str(entry.get('id')) == ref:
                sys.stdout.write(f"{entry.get('id')}\t{entry.get('name') or ref}\n")
                return 0
        die(f'پروژهٔ {ref} در حساب سوپابیس پیدا نشد')

    if command == 'envid':
        key = argv[3]
        entries = load_json(argv[2])
        items = entries if isinstance(entries, list) else entries.get('envs', [])
        for entry in items:
            if entry.get('key') == key:
                sys.stdout.write(str(entry.get('id')))
                return 0
        die(f'متغیر {key} پیدا نشد')

    if command == 'deploybody':
        root, project = argv[2], argv[3]
        body = {
            'name': project,
            'project': project,
            'target': 'production',
            'files': collect_files(root),
            'projectSettings': {'framework': None, 'buildCommand': None, 'installCommand': None},
        }
        sys.stdout.write(json.dumps(body, ensure_ascii=False))
        return 0

    if command == 'field':
        data = load_json(argv[2])
        value = data.get(argv[3]) if isinstance(data, dict) else None
        if value in (None, ''):
            die(f'فیلد {argv[3]} در پاسخ نبود')
        sys.stdout.write(str(value))
        return 0

    if command == 'error':
        data = load_json(argv[2])
        if isinstance(data, dict):
            message = data.get('message') or data.get('error') or data.get('error_description')
            if isinstance(message, dict):
                message = message.get('message') or message.get('code')
            if message:
                sys.stdout.write(str(message)[:400])
                return 0
        sys.stdout.write('')
        return 0

    die(f'زیردستور ناشناس: {command}')


if __name__ == '__main__':
    sys.exit(main(sys.argv))
