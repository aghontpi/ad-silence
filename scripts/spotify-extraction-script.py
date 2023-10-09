#!/usr/bin/env python3

# default strings
#  "Advertisement",
#  "Spotify",


outputfile = open("out-as-kotlin-formated.txt", 'w')


# todo: add language symbol as comment to right of the detection string
with open("decompiled-string-search.txt", "r") as iputfile:
   for line in iputfile:
       if 'ad_label">' in line or  'sas_interruption_title">' in line:
           processed =  line[line.index('">')+2:line.index('</string'):]
           print(" original: " +  line + " extracted:  " + processed)
           outputfile.write('"'+processed+'",\n')