import json
import gzip
import fileinput
import re

from base64 import b64decode, b64encode
from pprint import pprint

ENCODING = "utf-8"
BP_SIG = "SHAPEZ2-3-"

def dumpBP():
	PATTERN = r"" + BP_SIG + "(.*?)\$"
	for line in fileinput.input():
		match = re.match(PATTERN, line)
		if match == False: continue
		text = match.group(1)
		# pprint(text)
		val = gzip.decompress(b64decode(text.encode(ENCODING)))
		# pprint(val)
		data = json.loads(val)
		# pprint(data)
		return json.dumps(data, indent=2)

def main():
	result = dumpBP()
	print(result)

main()
