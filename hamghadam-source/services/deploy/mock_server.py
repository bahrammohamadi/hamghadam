#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""سرور جعلی Supabase/Vercel برای آزمون deploy.sh بدون شبکه و بدون راز واقعی.

فقط همان مسیرهایی را می‌شناسد که deploy.sh استفاده می‌کند و همهٔ درخواست‌ها را
برای بازرسی در GET /__state برمی‌گرداند.

متغیرهای محیطی:
  FORCE_SPLIT=1  → مسیر «اعمال دستور‌به‌دستور» را اجبار می‌کند (شبیه‌سازی رد شدن یک‌جا)
"""
import json
import os
import re
import sys
import threading

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from jsonutil import split_statements  # noqa: E402
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

FORCE_SPLIT = os.environ.get('FORCE_SPLIT') == '1'
REF = 'testref'
PROJECT = 'hamghadam-api'

STATE = {
    'envs': {},
    'queries': [],
    'deployment_files': [],
    'requests': [],
    'deployments': {},
    'polls': 0,
}
LOCK = threading.Lock()


def json_reply(handler, status, payload):
    body = json.dumps(payload, ensure_ascii=False).encode('utf-8')
    handler.send_response(status)
    handler.send_header('Content-Type', 'application/json')
    handler.send_header('Content-Length', str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


class Handler(BaseHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def log_message(self, *args):  # ساکت
        pass

    def _body(self):
        length = int(self.headers.get('Content-Length') or 0)
        raw = self.rfile.read(length) if length else b''
        try:
            return json.loads(raw.decode('utf-8')) if raw else {}
        except Exception:  # noqa: BLE001
            return {'__raw__': raw.decode('utf-8', 'replace')}

    def _record(self, method, body):
        with LOCK:
            STATE['requests'].append({
                'method': method,
                'path': self.path.split('?')[0],
                'auth': bool(self.headers.get('Authorization')),
                'body': body,
            })

    # ------------------------------------------------------------------ GET
    def do_GET(self):
        path = self.path.split('?')[0]
        self._record('GET', None)

        if path == '/__state':
            with LOCK:
                return json_reply(self, 200, STATE)

        if path == '/v1/projects':
            return json_reply(self, 200, [{'id': REF, 'name': 'hamghadam-test', 'region': 'eu-central-1'}])

        if path == f'/v1/projects/{REF}/api-keys':
            return json_reply(self, 200, [
                {'name': 'anon', 'api_key': 'anon-key-for-tests-1234567890'},
                {'name': 'service_role', 'api_key': 'service-role-not-used-here'},
            ])

        if path == f'/v9/projects/{PROJECT}':
            return json_reply(self, 200, {'id': 'prj_test', 'name': PROJECT})

        if path == f'/v9/projects/{PROJECT}/env':
            with LOCK:
                items = [{'id': f'env_{k}', 'key': k, 'value': v} for k, v in STATE['envs'].items()]
            return json_reply(self, 200, items)

        match = re.fullmatch(r'/v13/deployments/([A-Za-z0-9_]+)', path)
        if match:
            with LOCK:
                STATE['polls'] += 1
                ready = STATE['polls'] >= 2
            return json_reply(self, 200, {
                'id': match.group(1),
                'url': 'hamghadam-api-test.vercel.app',
                'readyState': 'READY' if ready else 'BUILDING',
            })

        if path == '/api/health':
            return json_reply(self, 200, {'status': 'ok', 'capabilities': {'auth': True, 'agent': True}})

        return json_reply(self, 404, {'message': f'not found: {path}'})

    # ----------------------------------------------------------------- POST
    def do_POST(self):
        path = self.path.split('?')[0]
        body = self._body()
        self._record('POST', body)

        if path == f'/v1/projects/{REF}/database/query':
            query = str(body.get('query', ''))
            if FORCE_SPLIT:
                # شمارش واقعی دستورها (با احترام به بدنهٔ $$ تابع plpgsql)
                if len(split_statements(query)) > 1:
                    return json_reply(self, 400, {'message': 'multiple statements are not allowed'})
            elif 'create table' not in query.lower():
                return json_reply(self, 400, {'message': 'unexpected query'})
            with LOCK:
                STATE['queries'].append(query)
            return json_reply(self, 201, [])

        if path == '/v11/projects':
            return json_reply(self, 200, {'id': 'prj_test', 'name': body.get('name')})

        if path == f'/v10/projects/{PROJECT}/env':
            key = body.get('key')
            with LOCK:
                if key in STATE['envs']:
                    return json_reply(self, 409, {'error': {'code': 'ENV_ALREADY_EXISTS'}})
                STATE['envs'][key] = body.get('value')
            return json_reply(self, 201, {'created': {'key': key}})

        if path == '/v13/deployments':
            files = body.get('files') or []
            names = sorted(item.get('file') for item in files)
            with LOCK:
                STATE['deployment_files'] = names
            if not any(name == 'api/health.js' for name in names):
                return json_reply(self, 400, {'error': {'message': 'missing api/health.js'}})
            return json_reply(self, 201, {'id': 'dpl_test1', 'url': 'hamghadam-api-test.vercel.app'})

        return json_reply(self, 404, {'message': f'not found: {path}'})

    # ---------------------------------------------------------------- PATCH
    def do_PATCH(self):
        path = self.path.split('?')[0]
        body = self._body()
        self._record('PATCH', body)
        match = re.fullmatch(rf'/v9/projects/{PROJECT}/env/(env_.+)', path)
        if match:
            with LOCK:
                key = match.group(1)[len('env_'):]
                if key in STATE['envs']:
                    STATE['envs'][key] = body.get('value')
                    return json_reply(self, 200, {'key': key})
            return json_reply(self, 404, {'error': {'message': 'env not found'}})
        return json_reply(self, 404, {'message': f'not found: {path}'})


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 0
    server = ThreadingHTTPServer(('127.0.0.1', port), Handler)
    server.daemon_threads = True
    print(server.server_address[1], flush=True)
    server.serve_forever()


if __name__ == '__main__':
    main()
