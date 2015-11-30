#!/bin/bash
mvn package
scp target/entities-0.0.1-SNAPSHOT.jar gsherma2@gsliscluster1.lis.illinois.edu:~/entities/lib/
rsync -a target/lib/* gsherma2@gsliscluster1.lis.illinois.edu:~/entities/lib/
