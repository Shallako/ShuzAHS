# Git Workflow

When making and applying changes in this repository:
1. Always create a new branch for changes before merging into `master` (e.g. `git checkout -b <branch-name>`).
2. Commit changes on that branch.
3. Check out `master` and merge the branch (`git checkout master && git merge <branch-name>`).
4. Push `master` to remote (`git push origin master`).
5. Delete the temporary branch (`git branch -d <branch-name>`).
