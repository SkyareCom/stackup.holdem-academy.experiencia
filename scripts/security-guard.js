const fs = require('fs');
const path = require('path');

const ROOT = process.cwd();
const SKIP_DIRS = new Set(['.git', 'node_modules', 'build', '.gradle']);
const TEXT_EXTS = new Set([
  '.js', '.mjs', '.cjs', '.html', '.css', '.md', '.txt', '.json', '.xml',
  '.yml', '.yaml', '.kts', '.gradle', '.properties', '.java', '.webmanifest',
  '.sh', '.ps1'
]);

const violations = [];

function rel(p) {
  return path.relative(ROOT, p).replace(/\\/g, '/');
}

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory() && SKIP_DIRS.has(entry.name)) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(full);
    else inspectFile(full);
  }
}

function inspectFile(file) {
  const r = rel(file);
  const base = path.basename(file);

  if (/^\.env(?:\.|$)/i.test(base) && base !== '.env.example') {
    violations.push(`${r}: environment file must not be committed`);
  }
  if (/\.(?:jks|keystore|p12|pfx)$/i.test(base)) {
    violations.push(`${r}: signing/credential container must not be committed`);
  }
  if (/^(?:credentials|service-account).*\.json$/i.test(base) || base === 'google-services.json') {
    violations.push(`${r}: credential/service configuration must not be committed`);
  }

  const ext = path.extname(file).toLowerCase();
  if (!TEXT_EXTS.has(ext) || fs.statSync(file).size > 2_000_000) return;

  const text = fs.readFileSync(file, 'utf8');
  const secretPatterns = [
    [/-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/, 'private key material'],
    [/\bAKIA[0-9A-Z]{16}\b/, 'AWS access key'],
    [/\bgh[pousr]_[A-Za-z0-9_]{30,}\b/, 'GitHub token'],
    [/\bgithub_pat_[A-Za-z0-9_]{20,}\b/, 'GitHub fine-grained token'],
    [/\bAIza[0-9A-Za-z_-]{35}\b/, 'Google API key'],
    [/\bsk-(?:proj-)?[A-Za-z0-9_-]{20,}\b/, 'API secret key']
  ];

  for (const [pattern, label] of secretPatterns) {
    if (pattern.test(text)) violations.push(`${r}: possible ${label}`);
  }

  if (r.startsWith('.github/workflows/')) {
    if (/contents\s*:\s*write/i.test(text)) {
      violations.push(`${r}: workflow requests contents: write`);
    }
    if (/permissions\s*:\s*write-all/i.test(text)) {
      violations.push(`${r}: workflow requests write-all permissions`);
    }
    if (/git\s+push\b/i.test(text)) {
      violations.push(`${r}: workflow performs git push`);
    }
    if (/pull_request_target\s*:/i.test(text)) {
      violations.push(`${r}: pull_request_target is forbidden for this repository`);
    }
    if (/secrets\s*:\s*inherit/i.test(text)) {
      violations.push(`${r}: inherited secrets are forbidden`);
    }
    if (/(?:curl|wget)[^\n|]*\|\s*(?:bash|sh)\b/i.test(text)) {
      violations.push(`${r}: remote script pipe-to-shell is forbidden`);
    }

    for (const line of text.split(/\r?\n/)) {
      const match = line.match(/^\s*uses:\s*([^\s@]+)@([^\s#]+)/);
      if (!match) continue;
      const action = match[1];
      const ref = match[2];
      if (action.startsWith('./') || action.startsWith('docker://')) continue;
      if (!/^[0-9a-f]{40}$/i.test(ref)) {
        violations.push(`${r}: action ${action} must be pinned to an immutable 40-character commit SHA`);
      }
    }
  }
}

walk(ROOT);

if (violations.length) {
  console.error('SECURITY GUARD FAILED');
  for (const item of violations) console.error(`- ${item}`);
  process.exit(1);
}

console.log('Security guard passed: credentials blocked, workflows read-only where appropriate, and external Actions pinned to immutable commit SHAs.');
