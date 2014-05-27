Javadoc Comparator
==================

Tool written in Java to compare pairs of javadocs. This application is executing UNIX system **diff** command and executing a **bash** script therefore it's **not working on Windows**.

Why?
----

It's interesting that javadoc maven plugin isn't generating the javadoc deterministic - the order of the methods is different therefore you can't simply use the diff command to compare javadoc. You have to remove all the date occurences (because there is a date when was the javadoc generated) and find the moved blocks and delete them from the output - that's what basically this application is doing.

Simply run `mvn test -DjavadocDir=yourDir` where `javadocDir` should have following structure:
javadocDir
  * first set of javadocs to compare
    * testjavadoc1.jar
    * testjavadoc2.jar
  * second set of javadocs
    * testjavadoc1.jar
    * testjavadoc2.jar

and so on...
