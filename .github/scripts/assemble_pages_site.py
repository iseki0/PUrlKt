from __future__ import annotations

import argparse
import html
import shutil
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--site-dir", required=True)
    parser.add_argument("--docs-dir", required=True)
    parser.add_argument("--channel", choices=("snapshot", "release"), required=True)
    parser.add_argument("--version-label", required=True)
    parser.add_argument("--release-version")
    return parser.parse_args()


def copy_tree(source: Path, destination: Path) -> None:
    if destination.exists():
        shutil.rmtree(destination)
    shutil.copytree(source, destination)


def release_sort_key(version: str) -> tuple:
    parts = []
    for token in version.split("."):
        if token.isdigit():
            parts.append((0, int(token)))
        else:
            parts.append((1, token))
    return tuple(parts)


def list_release_versions(site_dir: Path) -> list[str]:
    releases_dir = site_dir / "releases"
    if not releases_dir.exists():
        return []
    versions = [path.name for path in releases_dir.iterdir() if path.is_dir()]
    return sorted(versions, key=release_sort_key, reverse=True)


def render_versions_index(
    version_label: str,
    releases: list[str],
    has_snapshot: bool,
    base_prefix: str = "",
) -> str:
    latest_release = releases[0] if releases else None
    release_items = "\n".join(
        f'<li><a href="{base_prefix}releases/{html.escape(version)}/">{html.escape(version)}</a></li>'
        for version in releases
    ) or "<li>No release docs published yet.</li>"
    latest_link = (
        f'<a class="card" href="{base_prefix}releases/{html.escape(latest_release)}/">'
        f"<span>Latest release</span><strong>{html.escape(latest_release)}</strong></a>"
        if latest_release is not None
        else '<span class="card disabled"><span>Latest release</span><strong>Not published yet</strong></span>'
    )
    snapshot_link = (
        f'<a class="card" href="{base_prefix}snapshot/"><span>Snapshot</span>'
        f"<strong>{html.escape(version_label)}</strong></a>"
        if has_snapshot
        else '<span class="card disabled"><span>Snapshot</span><strong>Not published yet</strong></span>'
    )

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>PUrlKt Docs</title>
  <style>
    :root {{
      color-scheme: light;
      --bg: #f3efe6;
      --surface: #fffaf0;
      --ink: #1f2937;
      --muted: #6b7280;
      --line: #d6cfc2;
      --accent: #0f766e;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      font-family: "Georgia", "Times New Roman", serif;
      background:
        radial-gradient(circle at top left, rgba(15,118,110,0.12), transparent 28rem),
        linear-gradient(180deg, #f7f3ea 0%, var(--bg) 100%);
      color: var(--ink);
    }}
    main {{
      max-width: 58rem;
      margin: 0 auto;
      padding: 4rem 1.5rem 5rem;
    }}
    h1 {{
      margin: 0 0 0.75rem;
      font-size: clamp(2.5rem, 6vw, 4.5rem);
      line-height: 0.95;
      letter-spacing: -0.04em;
    }}
    p {{
      color: var(--muted);
      font-size: 1.05rem;
      line-height: 1.65;
      max-width: 42rem;
    }}
    .cards {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(16rem, 1fr));
      gap: 1rem;
      margin: 2rem 0 2.5rem;
    }}
    .card {{
      display: block;
      padding: 1.25rem;
      border: 1px solid var(--line);
      border-radius: 1rem;
      background: rgba(255, 250, 240, 0.86);
      text-decoration: none;
      color: inherit;
      box-shadow: 0 18px 45px rgba(31, 41, 55, 0.08);
      backdrop-filter: blur(8px);
    }}
    .card span {{
      display: block;
      color: var(--muted);
      font-size: 0.92rem;
      margin-bottom: 0.4rem;
    }}
    .card strong {{
      font-size: 1.15rem;
    }}
    .card.disabled {{
      opacity: 0.62;
      pointer-events: none;
    }}
    h2 {{
      margin-top: 3rem;
      font-size: 1.25rem;
    }}
    ul {{
      padding-left: 1.2rem;
    }}
    li + li {{
      margin-top: 0.5rem;
    }}
    a {{
      color: var(--accent);
    }}
  </style>
</head>
<body>
  <main>
    <h1>PUrlKt Docs</h1>
    <p>
      Stable API docs are published for tagged releases. A continuously updated snapshot
      is kept separately so master can still be inspected without replacing the stable site.
    </p>
    <section class="cards">
      {latest_link}
      {snapshot_link}
    </section>
    <section>
      <h2>Published releases</h2>
      <ul>
        {release_items}
      </ul>
    </section>
  </main>
</body>
</html>
"""


def render_root_redirect(releases: list[str], has_snapshot: bool) -> str:
    if releases:
        target = f"releases/{html.escape(releases[0])}/"
        label = html.escape(releases[0])
        message = f"Redirecting to latest stable docs ({label})."
    elif has_snapshot:
        target = "snapshot/"
        label = "snapshot"
        message = "Redirecting to snapshot docs."
    else:
        target = "versions/"
        label = "versions"
        message = "Redirecting to available docs."

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="refresh" content="0; url={target}">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>PUrlKt Docs</title>
  <script>location.replace({target!r});</script>
</head>
<body>
  <p>{message} If you are not redirected, <a href="{target}">open {label}</a>.</p>
</body>
</html>
"""


def main() -> None:
    args = parse_args()
    site_dir = Path(args.site_dir).resolve()
    docs_dir = Path(args.docs_dir).resolve()

    if not docs_dir.is_dir():
        raise SystemExit(f"Docs directory not found: {docs_dir}")

    site_dir.mkdir(parents=True, exist_ok=True)
    (site_dir / ".nojekyll").write_text("", encoding="utf-8")
    metadata_dir = site_dir / ".meta"
    metadata_dir.mkdir(exist_ok=True)
    snapshot_label_file = metadata_dir / "snapshot-version.txt"

    if args.channel == "snapshot":
        copy_tree(docs_dir, site_dir / "snapshot")
        snapshot_label_file.write_text(args.version_label, encoding="utf-8")
    else:
        if not args.release_version:
            raise SystemExit("--release-version is required when channel=release")
        copy_tree(docs_dir, site_dir / "releases" / args.release_version)

    releases = list_release_versions(site_dir)
    has_snapshot = (site_dir / "snapshot" / "index.html").exists()
    snapshot_label = (
        snapshot_label_file.read_text(encoding="utf-8").strip()
        if snapshot_label_file.exists()
        else "master-SNAPSHOT"
    )
    versions_dir = site_dir / "versions"
    versions_dir.mkdir(exist_ok=True)
    (versions_dir / "index.html").write_text(
        render_versions_index(snapshot_label, releases, has_snapshot, "../"),
        encoding="utf-8",
    )
    (site_dir / "index.html").write_text(
        render_root_redirect(releases, has_snapshot),
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
