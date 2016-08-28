#!/usr/bin/env bash -e

function jars {
    echo "---- $1"; shift
    local JARS=$(find $1 -name \*.jar | egrep $3 $2 | sed "s/^\.\///g")
    ls -l $JARS | awk '{printf "%-15s %s\n", $5, $9}'
    local SIZE=$[($(find $1 -name \*.jar | grep target | egrep $3 $2 | xargs stat -f "%z" | xargs | tr " " "+")+512)/1024]
    local DIRS=$(for f in $JARS; do echo $f | cut -d/ -f1,2; done)
    local CLASSES=$(for d in $DIRS; do find "$d/target/classes" -name \*.class | grep -v 'PackageBindings\$'; done | wc -l)
    local LINES=$(for d in $DIRS; do for file in `find "$d/src/main" -name \*.java`; do cat $file | tr -d "\r" | clang -E - 2>/dev/null | egrep -v "^#|^import|^package|^$"; done; done | wc -l)
    echo Total $SIZE KB, $CLASSES classes, $LINES lines, $[$LINES/$CLASSES] lines/class
}

jars "Command Line Applications" . "staging|build|test|mock|web|war|osgi|feature|tutorials" -v
jars "Web Applications" . "staging|build|test|mock|cli|command|osgi|war-bootstrap|feature|tutorials" -v
jars "OSGi Applications" . "staging|build|test|mock|cli|command|web|war|local|feature|tutorials" -v
jars "OSGi Embedded Bundles"  deployment "osgi-bundle-bootstrap-local"
jars "OSGi Standalone Bundles" . "staging|build|test|mock|cli|command|web|war|application|feature|tutorials" -v
jars "Features" features "staging|manifest|sink|commons|slf4j|java|remote|tutorials" -v
jars "Everything" . "staging|test|mock|build|tutorials" -v

