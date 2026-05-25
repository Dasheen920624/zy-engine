# Dify Runtime Boundary

Dify is an optional workflow and model-orchestration dependency of the development platform.
Its official Docker Compose project is deliberately not copied into this repository. The small
committed `compose.lock.yml` overlay pins default full-mode images by digest so upstream helper
tags such as `latest` cannot silently drift between deployments.

`../scripts/bootstrap-runtime.sh` checks out the pinned official release to:

```text
${MEDKERNEL_RUNTIME_ROOT}/dify/v1.14.0/
```

The official release is displayed as `v1.14.0`; its upstream Git tag is `1.14.0`. The
bootstrap configuration keeps these as `DIFY_VERSION` and `DIFY_GIT_REF` so the human-readable
runtime directory and the reproducible checkout remain explicit.

The bootstrap script creates the upstream `docker/.env` from Dify's own example and applies
only local ingress ports (`8090` for HTTP and `8443` for HTTPS) plus randomly generated
PostgreSQL, Redis, session, sandbox, and plugin keys on first creation. Its PostgreSQL, Redis,
vector store, sandbox, and plugin service topology remains governed by the upstream release;
validated default-topology image digests are governed by `compose.lock.yml`.

## Operations

```bash
./deploy/docker/scripts/up.sh full
./deploy/docker/scripts/healthcheck.sh full
./deploy/docker/scripts/down.sh full
```

If a Dify runtime was bootstrapped from upstream example credentials but has not yet received
users, apps, or provider credentials, rotate it once before using it:

```bash
./deploy/docker/scripts/rotate-dify-initial-secrets.sh --confirm-unconfigured
```

Do not run this rotation after configuring Dify: changing its `SECRET_KEY` can make encrypted
stored settings unreadable. New installations created through `bootstrap-runtime.sh` do not
need this extra step.

To inspect or adapt Dify for a target server, edit the runtime copy at
`${MEDKERNEL_RUNTIME_ROOT}/dify/v1.14.0/docker/.env`; it contains runtime secrets and must not
be committed. The sample local ingress settings are documented in `.env.override.example`.

MedKernel application records remain authoritative in its own PostgreSQL service. Stopping Dify
must not delete or replace MedKernel business data.

When upgrading Dify or selecting an alternate vector-store service, refresh the image-lock
overlay as a reviewed deployment change and repeat the full health and backup checks.
