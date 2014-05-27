Javadoc Comparator
==================

Tool written in Java to compare pairs of javadocs. This application is executing UNIX system **diff** command and executing a **bash** script therefore it's **not working on Windows**.

Why and why the $#$@ this way?
------------------------------

It's interesting that javadoc maven plugin isn't generating the javadoc deterministic - the order of the methods is different therefore you can't simply use the diff command to compare javadoc. You have to remove all the date occurences (because there is a date when was the javadoc generated) and find the moved blocks and delete them from the output - that's what basically this application is doing. With a slight modification you can probably use this application for almost everything, according to comparing with diff.

I've chosen this approach because I just wanted to automatize the things that I've done manually. It was much easier to call a few shell commands than reinvent the wheel using pure java.

Prerequisites
--------------

  * maven
  * java
  * linux
  * a pair of javadoc jar files :-)

How to use
----------

Simply run `mvn test -DjavadocDir=yourDir` where `javadocDir` should have following structure:<br/>
javadocDir
  * first set of javadocs to compare
    * testjavadoc1.jar
    * testjavadoc2.jar
  * second set of javadocs
    * testjavadoc1.jar
    * testjavadoc2.jar

and so on...

This application is using a python script found in [this stackoverflow answer](http://stackoverflow.com/a/1400664) and was a bit modified for the purposes of this test. 

Expected output
---------------

Nothing! In case that the javadocs are the same. If some test fails maven will produce something like this:
`Failed tests:   compareJavadocs[\/home\/avano\/work\/javadoc\/test\/test1](com.github.avano.javadoc.JavadocsTest): Final diff file size expected:<0> but was:<5277>
  compareJavadocs[\/home\/avano\/work\/javadoc\/test\/test2](com.github.avano.qa.javadoc.JavadocsTest): Final diff file size expected:<0> but was:<2162607>`

Then you can check the diff output in particular folder:
  * removeMovedWithoutHeaders.diff - file for computers, it contains just the text without filename headers and so on
  * removeMovedWithHeaders.diff - file for humans :-) - when you want to check in which file the difference is 

Performance
-----------

It takes about 3 minutes to compare two 22MB javadoc jars. Main part of the time is because of the running bash script that is removing all the dates from the javadoc html pages. Therefore you can see the test output in the maven logs just to be sure that it's working :-)



