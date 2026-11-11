# Contributing to ResilienceLab

Thanks for your interest! This is currently a learning and portfolio project, but
suggestions and issues are welcome.

## Branching strategy

- `main`: stable releases only.
- `develop`: integration branch; pull requests target this branch.
- `feature/*`, `fix/*`, `docs/*`, `test/*`: short-lived working branches.

Example: `feature/order-service`, `fix/payment-idempotency`.

## Commit messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(order): implement order creation
fix(inventory): prevent negative stock
docs(architecture): add sequence diagram
test(payment): add duplicate payment test
ci: add maven build workflow
```

Allowed types: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `ci`, `build`.

## Pull requests

1. Create a branch from `develop`.
2. Keep the change focused on one feature or fix.
3. Make sure the build and tests pass.
4. Update documentation when behaviour changes.
5. Open a pull request against `develop`.

## Secrets

Never commit passwords, tokens, `.env` files or credentials. Use `.env.example` to
document required variables.
