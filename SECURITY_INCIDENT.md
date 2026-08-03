# SECURITY INCIDENT: Leaked OpenRouter API Key

What happened
- A secret OpenRouter API key was committed to the repository in commit 223267de593525bc894778323073ad61bafc3f82 and is therefore present in the Git history.

What I changed so far (automated)
- Replaced the hardcoded value in Backend/ai_resume_analyzer/src/main/resources/application.properties with an environment-variable based value:
  openrouter.api.key=${OPENROUTER_API_KEY:your_key}
- Commit: Use environment variable for OpenRouter API key
  https://github.com/SwetamKumar/Ai_Resume_Analyzer/commit/6b9ab0d2860853119401b70d3205e64818d1c9f5

Required actions (you must run these locally — they cannot be completed from this bot):
1) Immediately revoke/rotate the exposed OpenRouter key
   - Treat the key as compromised: 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
   - Create a new key in OpenRouter and update deployments/CI to use the new value.

2) Remove the secret from the repository history (run the script below locally)
   - Script path in this repo: scripts/cleanup_secret.sh
   - Usage (example):
     SECRET="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" ./scripts/cleanup_secret.sh

   - The script uses git-filter-repo to replace the secret everywhere in history and then force-pushes a cleaned mirror back to origin. You must have push permissions on the repository and git-filter-repo installed (pip install git-filter-repo).

3) Communicate to collaborators
   - After you rewrite history, everyone with clones must re-clone the repository (recommended) or follow recovery steps. The safest instruction to give collaborators is: re-clone from origin into a new directory.

4) Post-cleanup tasks
   - Update CI/servers to use the new OpenRouter key stored as an environment variable (OPENROUTER_API_KEY) or in your secret store.
   - Check GitHub Security → Secret scanning for alerts and act on them.
   - If the repository was public, consider rotating any other keys that may have been used with the leaked key.

Notes and caveats
- Rewriting history is disruptive: the script force-pushes cleaned history and will require all contributors to re-clone.
- Forks and external clones you do not control may still contain the secret; rotating the key is the only full mitigation for public leaks.

If you'd like, I can also:
- Draft a collaboration notification message you can paste into Slack / email (I can add it to this file or create an issue). 
- Provide a GitHub Actions workflow example that uses repository secrets to inject OPENROUTER_API_KEY into deployments.
