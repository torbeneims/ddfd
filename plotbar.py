#!/usr/bin/env python3
import matplotlib.pyplot as plt
import numpy as np
import json
import sys

if len(sys.argv) != 3:
	print("USAGE: %s <input file> <output file>" % sys.argv[0])
	exit()

f = open(sys.argv[1])
data = json.load(f)

for res in data['results']:
	plt.bar(x=res['command'], height=res['mean'], yerr=res['stddev'], capsize=10)

plt.ylim([0, None])
plt.xlabel('Command')
plt.ylabel('Execution time in s')
plt.savefig(sys.argv[2])

# plt.show()

f.close()

