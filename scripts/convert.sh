#!/bin/bash

# NB: this converter uses master-new branch of shower

JARDIR=$(dirname $(grealpath $0))
JAR="$JARDIR/clj-shower-1.0.1-standalone.jar"
SHOWER=$1
if [[ -z "$SHOWER" ]]
then
    echo "usage: convert file.shower" 1>&2
    exit 1
fi
if [[ ! $SHOWER =~ .shower$ ]]
then
    echo "first argument must be .shower file" 1>&2
    exit 1
fi
SHOWERDIR=$(dirname $(grealpath $SHOWER))
NAME=$(basename $SHOWER .shower)
HTML=$NAME.html
LOCK=/tmp/$NAME.lock
RELOAD="tell application \"Safari\" to open location \"http://localhost:9000/$NAME/\""

# if ! curl -sf localhost:9000 > /dev/null
# then
#     cd $JARDIR
#     nohup mongoose -p 9000 2>&1 >/dev/null &
# fi

if shlock -f $LOCK -p $$
then
    java -jar $JAR --shower $SHOWER --html $SHOWERDIR/$HTML 2>&1
    [[ ! -e $SHOWERDIR/index.html ]] && ln -s $HTML $SHOWERDIR/index.html
    osascript -e "$RELOAD"
    rm $LOCK
fi

