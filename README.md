Entities
========

Code for entity linking and information retrieval.

Configuration Files
-------------------

Configuration files consist of lines of the form

> property: value

At the moment, there's no documentation for which properties a run requires. Looking through the run class (in the edu.gslis.main package) should make it clear.

Run Files
---------

Run files should look something like this:

> java -Djava.library.path=/path/to/liblemur_jni/directory/ -cp /path/to/lib/directory/:/path/to/logback.xml/directory edu.gslis.main.RunClass /path/to/config/file

Notes:
- You must have Indri installed
- The `lib` directory should contain the jars in `target/lib` as well as the `entities-0.0.1-SNAPSHOT.jar` file in `target`. Alternatively, `entities-0.0.1-SNAPSHOT-jar-with-dependencies.jar` alone will work, but likely means longer uploads to the server using rsync.
- The classpath should include the directory containing the `logback.xml` file; leaving out this file will result in logging to `stdout`, which will mess up retrieval output.
- You may need to specify more memory for Java with `_JAVA_OPTIONS=-Xmx1g` before the run line, replacing 1 with the needed amount of memory
