#!/usr/bin/env bash
set -euo pipefail

echo "🛠  Building backend (this takes ~1-2 min on first run)..."
cd "$(dirname "$0")/.."
(cd backend && mvn -q clean package -DskipTests)

chmod +x scripts/*.sh || true

# Copy env template if user hasn't created one yet
if [ ! -f .env ]; then
  cp .devcontainer/.env.example .env
  echo "📝 Created .env from template (.devcontainer/.env.example)."
fi

cat <<'EOF'

✅ Devcontainer ready.

▶  Start the backend (in a terminal):
     cd backend && java \
       -Dsecurity.enabled=false \
       -Dazure.openai.endpoint=${AZURE_OPENAI_ENDPOINT} \
       -Dazure.openai.api-key=${AZURE_OPENAI_API_KEY} \
       -jar target/burnout-backend-0.0.1-SNAPSHOT.jar

▶  In another terminal, run the seed → reshape demo:
     bash scripts/seed-demo.sh http://localhost:8080            # BEFORE (stress 100)
     bash scripts/seed-demo.sh http://localhost:8080 after      # BEFORE → reshape

▶  Open the forwarded port 8080:
     /              landing page
     /checkin.html  enter   roryp  +  roryp/burnout-app
     /flamegraph.html?repo=roryp/burnout-app&userId=roryp
     /study.html    click "Load Data"

(If AZURE_OPENAI_* are dummy values, the agents fall back to deterministic
 responses — the full demo still works end-to-end.)
EOF
