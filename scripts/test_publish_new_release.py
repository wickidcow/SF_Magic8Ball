"""Execute the real immutable release guard against an isolated gh substitute."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

GUARD = Path(__file__).resolve().with_name("publish_new_release.sh")
SHA = "c" * 40


class ImmutableReleaseTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        (self.root / "dist").mkdir()
        (self.root / "dist" / "SF_Magic8Ball1.0.3.jar").write_bytes(b"fixture jar")
        self.log = self.root / "gh-operations.log"
        gh = self.root / "gh"
        gh.write_text(
            "#!/usr/bin/env python3\n"
            "import os, sys\n"
            "with open(os.environ['GH_LOG'], 'a', encoding='utf-8') as stream:\n"
            "    stream.write(' '.join(sys.argv[1:]) + '\\n')\n"
            "if sys.argv[1:3] == ['release', 'view']:\n"
            "    sys.exit(0 if os.getenv('GH_EXISTING') == '1' else 1)\n"
            "sys.exit(0 if sys.argv[1:3] == ['release', 'create'] else 10)\n",
            encoding="utf-8",
        )
        gh.chmod(0o755)
        self.env = {
            **os.environ,
            "PATH": f"{self.root}{os.pathsep}{os.environ.get('PATH', '')}",
            "GH_LOG": str(self.log),
            "GITHUB_REPOSITORY": "wickidcow/SF_Magic8Ball",
            "GITHUB_EVENT_NAME": "workflow_dispatch",
            "GITHUB_REF": "refs/tags/v1.0.3",
            "GITHUB_REF_NAME": "v1.0.3",
            "GITHUB_SHA": SHA,
            "EXPECTED_SOURCE_SHA": SHA,
            "RELEASE_CONFIRMED": "true",
            "ARTIFACT_VERSION": "1.0.3",
            "OUTPUT_NAME": "SF_Magic8Ball1.0.3.jar",
        }

    def execute(self, **changes):
        proc = subprocess.run(
            ["bash", str(GUARD)],
            cwd=self.root,
            env={**self.env, **changes},
            text=True,
            capture_output=True,
            check=False,
        )
        commands = self.log.read_text(encoding="utf-8").splitlines() if self.log.exists() else []
        return proc, commands

    def test_new_version_only_has_nonmutating_create(self):
        proc, commands = self.execute()
        self.assertEqual(0, proc.returncode, proc.stderr)
        self.assertEqual(2, len(commands))
        self.assertIn("release view v1.0.3", commands[0])
        self.assertIn("release create v1.0.3 dist/SF_Magic8Ball1.0.3.jar", commands[1])
        self.assertIn("--verify-tag", commands[1])
        self.assertNotIn("--clobber", " ".join(commands))
        self.assertNotIn("release upload", " ".join(commands))
        self.assertNotIn("release edit", " ".join(commands))

    def test_existing_release_is_never_changed(self):
        proc, commands = self.execute(GH_EXISTING="1")
        self.assertNotEqual(0, proc.returncode)
        self.assertEqual(1, len(commands))
        self.assertIn("already exists", proc.stderr)

    def test_branch_and_tag_pushes_do_not_publish(self):
        for event, ref in [("push", "refs/tags/v1.0.3"), ("workflow_dispatch", "refs/heads/master")]:
            with self.subTest(event=event, ref=ref):
                proc, commands = self.execute(GITHUB_EVENT_NAME=event, GITHUB_REF=ref)
                self.assertNotEqual(0, proc.returncode)
                self.assertEqual([], commands)

    def test_explicit_authorization_is_required(self):
        proc, commands = self.execute(RELEASE_CONFIRMED="false")
        self.assertNotEqual(0, proc.returncode)
        self.assertEqual([], commands)

    def test_source_lease_cannot_be_stale(self):
        for expected in ["d" * 40, "", "short"]:
            with self.subTest(expected=expected):
                proc, commands = self.execute(EXPECTED_SOURCE_SHA=expected)
                self.assertNotEqual(0, proc.returncode)
                self.assertEqual([], commands)

    def test_tag_name_and_version_must_be_exact(self):
        for change in [{"GITHUB_REF_NAME": "v1.0.2"}, {"ARTIFACT_VERSION": "1.0.2"}]:
            with self.subTest(change=change):
                proc, commands = self.execute(**change)
                self.assertNotEqual(0, proc.returncode)
                self.assertEqual([], commands)

    def test_jar_identity_is_verified(self):
        for name in ["wrong.jar", "SF_Magic8Ball1.0.2.jar"]:
            with self.subTest(name=name):
                proc, commands = self.execute(OUTPUT_NAME=name)
                self.assertNotEqual(0, proc.returncode)
                self.assertEqual([], commands)

    def test_missing_jar_invalid_sha_and_wrong_repository_fail_closed(self):
        cases = [
            {"OUTPUT_NAME": "SF_Magic8Ball1.0.3.jar", "ARTIFACT_VERSION": "1.0.3", "GITHUB_REPOSITORY": "foreign/other"},
            {"GITHUB_SHA": "not-a-commit"},
        ]
        (self.root / "dist" / "SF_Magic8Ball1.0.3.jar").unlink()
        cases.append({})
        for case in cases:
            with self.subTest(case=case):
                proc, commands = self.execute(**case)
                self.assertNotEqual(0, proc.returncode)
                self.assertEqual([], commands)


if __name__ == "__main__":
    unittest.main()
