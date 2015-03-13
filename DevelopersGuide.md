# Developers Guide #

## Typical workflow ##

  1. Find an [issue](http://code.google.com/p/droidweight/issues/) that you want to fix.
  1. Download the code using [mercurial](http://mercurial.selenic.com) using `hg clone https://code.google.com/p/droidweight`
  1. If at all possible, write a unit test to show the problem. (Unit tests are about to be added to the project at the time of writing).
  1. Hack away at the code.
  1. Format any source code that you have edited (in eclipse use `Source > Format`).
  1. Check that all unit tests are passed.
  1. Commit your changes to your local repo using `hg commit -m "USEFUL MESSAGE"`.
  1. Create a patch (a patch is a file with the changes, often ending in ".diff") using `hg export tip > issueX.diff`.
  1. Find the issue on Google Code and add a comment, and attach the patch file to that comment.
  1. One of the DroidWeight developers will review the patch and thank you greatly.

## Repositories ##

Main code trunk: `hg clone https://code.google.com/p/droidweight`

Wiki: `hg clone https://code.google.com/p/droidweight.wiki`