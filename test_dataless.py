#!/usr/bin/env python
import locale
import os
import pprint
import sys
import time
import traceback

import Dataless
import Memory

locale.setlocale(locale.LC_ALL, "en_US")

def pretty_float(value, precision=1):
    return locale.format("%%0.%df" % precision, float(value), grouping=True)

def pretty_int(value):
    return locale.format("%d", int(value), grouping=True)

#engine = Dataless.Dataless("/qcwork/datalessSTUFF/littlesdataless") # EVERYTHING!
engine = Dataless.Dataless("/qcwork/datalessSTUFF/littlesANMO") # just ANMO

start_time = time.time()
try:
    engine.process()
except Exception, e:
    print "Caught an Exception at:"
    print "  file '%s'" % engine.dataless_file
    print "  %d lines skipped" % engine.skipped
    print "  on line %d of %d" % (engine.count, engine.total)
    print "    [%s]" % engine.line
    print
    print "Exception Details:"
    exc_type,exc_value,exc_traceback = sys.exc_info()                                               
    print traceback.format_exc()


end_time = time.time()

time.sleep(1.0)
mem = Memory.Memory()
mem_summary = {
    "Memory"     : pretty_int(mem.memory()),
    "Resident"   : pretty_int(mem.resident()),
    "Stack Size" : pretty_int(mem.stacksize()),
}
max_key = max(map(len, mem_summary.keys()))
max_value = max(map(len, mem_summary.values()))

print "Memory Stats"
print "------------"
for k,v in sorted(mem_summary.items()):
    print " %s : %s" % (k.rjust(max_key), v.rjust(max_value))
print
print "Total Time:", end_time - start_time, "seconds"

