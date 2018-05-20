# robo-refactor

The purpose of this project is to support cross-project refactoring.  Currently, only
github projects are supported, though it should not be difficult to add vsts.  On execute,
the each project in the list is cloned, the refactoring is applied, and if not in dry run
mode, a branch is pushed and a PR created.

To create a new refactoring, add a class which implements com.blackbaud.refactor.Refactor
to the com.blackbaud.refactor.robo package (see that package for an example).

Next, go to com.blackbaud.refactor.RoboRefactor and create a method which applies the 
multi-project refactor to your implementation (see `refactorBlackbaudSpringApplication` 
for an example) and invoke that method within the `run` method.

To execute, three inputs are required:
 * git account user
 * git account token
 * a file named `repo.list` in the root directory of the project which contains a list of the 
   repositories to apply the refactoring to, one per line

To generate a GitHub personal access token, go [here](https://github.com/settings/tokens) and click 
`Generate new token` (you only need `repo` scope for this purpose).

Initially, you will probaby want to do a dry run by calling `MultiProjectRefactor:dryRun`, which
iterates over the projects and applies the refactoring but does not push the branch or create
the PR.

Then, run via the command line...
```
gw bootRun -Pargs="<github user> <github token>"
```

