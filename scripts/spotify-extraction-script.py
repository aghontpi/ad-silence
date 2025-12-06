#!/usr/bin/env python3

# default strings
#  "Advertisement",
#  "Spotify",


outputfile = open("out-as-kotlin-formated.txt", 'w')


# todo: add language symbol as comment to right of the detection string
with open("decompiled-string-search.txt", "r") as iputfile:
   for line in iputfile:
       if '"advertisement">' in line and '</string>' in line:
           processed =  line[line.index('">')+2:line.index('</string>'):]
           print(" original: " +  line + " extracted:  " + processed)
           outputfile.write('"'+processed+'",\n')