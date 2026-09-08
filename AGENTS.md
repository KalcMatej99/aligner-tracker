# Aligner Tracker execution

Read docs/product.md, docs/architecture.md, docs/roadmap.md and your assigned Forgejo issue before changes. Repository: https://forgejo.server.matejkalc.com/matejkalc/aligner-tracker. Phone app module `app` and Wear OS companion module `wear`, application ID org.alignertracker.app. No competitor code/assets/content. No secrets, network permission, analytics or background ticking service.

Use independent scoped agent lanes only; at most two writers. Agree public API changes with coordinator. Stage only owned files; never overwrite another agent's edits. Link commits to issues. Run meaningful focused tests then formatting, lint and build at integration. Close an issue only with acceptance evidence; device-dependent gates stay open. Create concrete new issues for material findings, not speculative cleanup. No release signing/upload without user-owned key and explicit release scope.
