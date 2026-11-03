This guide is an exact copy of the same documented located [in our official docs](https://ui.docs.kafbat.io/development/contributing). If there are any differences between the documents, the one located in our official docs should prevail.

This guide aims to walk you through the process of working on issues and Pull Requests (PRs).

Bear in mind that you will not be able to complete some steps on your own if you do not have a “write” permission. Feel free to reach out to the maintainers to help you unlock these activities.

# General recommendations

Please note that we have a code of conduct (`CODE-OF-CONDUCT.md`). Make sure that you follow it in all of your interactions with the project.

# Issues

## Choosing an issue

There are two options to look for the issues to contribute to. <br/>
The first is our ["Up for grabs"](https://github.com/orgs/kafbat/projects/2) board. There the issues are sorted by a required experience level (beginner, intermediate, expert).

The second option is to search for ["good first issue"](https://github.com/kafbat/kafka-ui/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)-labeled issues. Some of them might not be displayed on the aforementioned board, or vice versa.

You also need to consider labels. You can sort the issues by scope labels, such as `scope/backend`, `scope/frontend` or even `scope/k8s`. If any issue covers several specific areas, and you do not have a required expertise for one of them, just do your part of work — others will do the rest.

## Grabbing the issue

There is a bunch of criteria that make an issue feasible for development. <br/>
The implementation of any features and/or their enhancements should be reasonable, must be backed by justified requirements (demanded by the community, [roadmap](https://ui.docs.kafbat.io/project/roadmap) plans, etc.). The final decision is left for the maintainers' discretion.

All bugs should be confirmed as such (i.e. the behavior is unintended).

Any issue should be properly triaged by the maintainers beforehand, which includes:
1. Having a proper milestone set
2. Having required labels assigned: accepted label, scope labels, etc.

Start working on an issue only after a maintainer has **assigned it to you**. No assignee means not yet. Ask to be assigned in the issue, then wait. Opening the issue yourself does not change this, and bot-added labels do not count as triage.

## Working on the issue

Every issue marked “in-progress” is assigned to the person working on it.
To keep the status of the issue clear to everyone, please keep the card's status updated ("project" card to the right of the issue should match the milestone’s name).

## Setting up a local development environment

Please refer to [this guide](https://ui.docs.kafbat.io/development/contributing).

# AI-assisted contributions

Use whatever tools you like, on one condition: you could have made the change without them. If you do not know the language or this codebase well enough to write the change yourself, do not open the PR. The tool does not make you the author; it makes you a relay, and we do not review relays. When asked, you must be able to explain every line and design decision in your PR. "The tool did it" is not an answer. If you cannot answer a design question about your PR, we close it instead of iterating on it.

In practice:
- Explain **why** in your description; the diff shows what changed. If the description only narrates changes ("updated X to support Y") without giving a reason, it reads as generated, and we assume nobody read the diff.
- Run the real build: `./gradlew build`, including checkstyle. "Tests pass" in the checklist means this repository's build passed on your machine. It is a statement of fact, not a ritual.
- If you fix a bug, reproduce it by hand in a running application before writing code. A unit test written alongside the fix is not enough. You do not have to be affected by the bug yourself, but you must have seen it.
- Fix the cause where it lives. A configuration option that hides a symptom is not a fix. Changing a default or an existing behavior breaks someone's setup; agree on it with a maintainer in the issue before writing it. Ticking the breaking-change box in the PR is disclosure, not approval.
- Use comments for constraints the code cannot express. Comments that narrate the next line, restate a method name, or document test fixtures are removed in review. A PR made of them gets the `meta/slop` label and is closed.
- Explain the root cause before tuning a performance problem. Measure changes on the workload described in the linked issue. Constants tuned on one setup are not evidence.
- Test the behavior you changed, through its public surface. Tests that reach into internals by reflection or cover code the PR did not touch are padding, not coverage.

# Pull Requests

## Branch naming

In order to keep branch names uniform and easy-to-understand, please use the following conventions for branch naming.

Generally speaking, it is a good idea to add a group/type prefix to a branch; e.g.,
if you are working on a specific branch, you could name it `issues/xxx`.

Here is a list of good examples:<br/>
`issues/123`<br/>
`feature/feature_name`<br/>
`bugfix/fix_thing`<br/>

## Code style

Java: There is a file called `checkstyle.xml` in project root under `etc` directory.<br/>
You can import it into IntelliJ IDEA via Checkstyle plugin.

## Naming conventions

REST paths should be written in **lowercase** and consist of **plural** nouns only.<br/>
Also, multiple words that are placed in a single path segment should be divided by a hyphen (`-`).<br/>

Query variable names should be formatted in `camelCase`.

Model names should consist of **plural** nouns only and should be formatted in `camelCase` as well.

## Creating a PR

When creating a PR please do the following:
1. In commit messages use these [closing keywords](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue#linking-a-pull-request-to-an-issue-using-a-keyword).
2. Link an issue(-s) via "linked issues" block.
3. Set the PR labels. Ensure that you set only the same set of labels that is present in the issue, and ignore yellow `status/` labels.
4. If the PR does not close any of the issues, the PR itself might need to have a milestone set. Reach out to the maintainers to consult.
5. Assign the PR to yourself. A PR assignee is someone whose goal is to get the PR merged.
6. Add reviewers. As a rule, reviewers' suggestions are pretty good; please use them.
7. Upon merging the PR, please use a meaningful commit message, task name should be fine in this case.
8. Keep each PR to one change. Do not bundle issues, stack branches that all target `main`, or slip refactoring into a bugfix. The diff is the smallest one that fixes the issue. If you rewrote a class to change a line, the rewrite is what gets reviewed, and it gets closed.
9. Do not force-push a reviewed branch. That orphans every review comment. Add commits; we squash on merge.
10. Do not open PRs in batches. We maintain this project in spare time. Five PRs in a day get paused until the queue clears, however good they are.
11. Check open PRs before claiming an issue. Do not open a competing PR for an issue already being worked on.

### Pull Request checklist

1. When composing a build, ensure that any install or build dependencies have been removed before the end of the layer.
2. Update the `README.md` with the details of changes made to the interface. This includes new environment variables, 
exposed ports, useful file locations, and container parameters.

## Reviewing a PR

Maintainers may close a PR without a full review if it breaks the rules above: no assigned issue, a description that does not match the diff, unrequested scope, or an author who cannot explain the change. The `meta/slop` label marks a pattern, not a single mistake. A PR closed with that label will not be reopened with the same code.

### Pull Request reviewer checklist

1. The linked issue is assigned to the author.
2. The description explains why and matches the diff.
3. The PR contains one change within the issue's scope.
4. The real build passes in CI, and checklist statements are true.
5. Comments explain constraints, not the code they accompany.
6. The author can answer design questions about every part of the diff.

# Security

Send security findings through [private vulnerability reporting](SECURITY.md). Never use public issues or PRs, including "hardening" PRs that would reveal a problem while a report is being handled. Duplicate reports are closed. Credit goes to the first reporter.

Information this repository already publishes is not a vulnerability: class names, dependency versions, stack traces of an open-source product. Severity is set by maintainers after verification; a scanner's or a model's rating is not evidence.
