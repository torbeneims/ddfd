cvx #!/usr/bin/env python3
import matplotlib.pyplot as plt
import numpy as np
import json
import sys
from itertools import groupby

# Serialize dicationary without specified key
def filter(dict, key):
	temp = dict.copy()
	temp.pop(key)
	return str(temp)

if len(sys.argv) != 4:
	print("USAGE: %s <input file> <output file> <x param>" % sys.argv[0])
	exit()


xparam = sys.argv[3]
f = open(sys.argv[1])
data = json.load(f)

# Group dataset by settings
list = sorted(data['results'], key=lambda x: filter(x['parameters'], xparam))
groupinfo = groupby(list, lambda x: filter(x['parameters'], xparam))

# Plot every setting
for key, group in groupinfo:
	print(key)

	temp = [x for x in group] # Convert to array

	xval = [ float(x['parameters'][xparam]) for x in temp ]
	yval = [ x['mean'] for x in temp ]
	err = [ x['stddev'] for x in temp ]

	print(xval)
	print(yval)

	plt.errorbar(x=xval, y=yval, yerr=err, label=key)

plt.ylim([0, None])
plt.xlabel(xparam)
plt.ylabel('Execution time in s')
plt.legend()
plt.savefig(sys.argv[2])

# plt.show()

f.close()

