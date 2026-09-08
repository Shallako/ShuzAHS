# Git Workflow

When making and applying changes in this repository:
1. Always create a new branch for changes before merging into `master` (e.g. `git checkout -b <branch-name>`).
2. Make changes and run all tests (`./gradlew clean test`) to ensure all tests are successful BEFORE merging into `master`.
3. Commit changes on the feature branch.
4. Check out `master` and merge the branch (`git checkout master && git merge <branch-name>`).
5. Run all tests again (`./gradlew clean test`) to ensure all tests are successful AFTER merging into `master`.
6. Push `master` to remote (`git push origin master`).
7. Delete the temporary branch (`git branch -d <branch-name>`).
