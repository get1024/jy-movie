# Support worker panel

## Development

Use Node.js 24 LTS (Node 22+ supported) and npm. This project stays on Vue 2.7 / Element UI 2 to preserve the existing interface.

```sh
npm ci
cp .env.example .env.local
npm run serve
```

Set `VUE_APP_API_URL` in `.env.local` if the backend is not at `http://localhost:8888/api`. These values are compiled into the browser bundle and must not contain secrets. Restart the dev server after changes; rebuild before deploying production environment changes.

## Verification

```sh
npm test
npm run build
npm audit --omit=dev
npm audit
```

Tests use Node's built-in test runner. Request tests run real Axios adapters without a backend. Component regression tests stub APIs; they do not replace browser/database integration tests. The development server binds to `127.0.0.1` by default. Production source maps are disabled. The production output is `dist/`. Configure the static host to fall back to `index.html` for history-mode routes.

## Maintenance and security

Vue 2 is end-of-life and Vue CLI is in maintenance mode. The compatible update to Vue CLI 5 / Webpack 5 allows modern Node without the OpenSSL legacy-provider flag, but does not remove the need for a future Vue 3 / Element Plus migration.

The current runtime audit reports two low-severity findings from the Vue 2 parser advisory (Vue and Element UI). The complete development dependency audit also includes known Vue CLI/compiler/dev-server and transitive build-tool vulnerabilities. Never compile untrusted Vue templates, do not expose the development server to untrusted networks, and serve only the built `dist/` artifacts in production. Do not use `npm audit fix --force`: its suggested framework downgrades/major upgrades break this application. Re-run audits as advisories change.

References: [Vue 2.7 migration](https://v2.vuejs.org/v2/guide/migration-vue-2-7.html), [Vue CLI 5 migration](https://cli.vuejs.org/migrations/migrate-from-v4.html), [Vue 2 end of life](https://v2.vuejs.org/eol/).
