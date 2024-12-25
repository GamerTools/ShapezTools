import sys
import json
import gzip
import fileinput

from base64 import b64decode, b64encode
from pprint import pprint

ENCODING = "utf-8"
BP_SIG = "SHAPEZ2-2-"

SIGNAL_TYPE = "ConstantSignalDefaultInternalVariant"

EMPTY_BP = """{
	"V": 1105,
	"BP": {
  	"$type": "Building",
  	"Icon": {"Data": ["icon:Buildings", null, null, "shape:CuCuCuCu"]},
  	"Entries": [],
  	"BinaryVersion": 1105
	}
}"""

def encodeShape(shape):
	value = "\x06\x01\x01" + chr(len(shape)) + "\x00"  + shape
	# pprint(value)
	return b64encode(value.encode(ENCODING)).decode(ENCODING)
	
MAX_X = 16

def makeConstants():
	bp = json.loads(EMPTY_BP)
	data = bp["BP"]["Entries"]
	num = 0
	for line in fileinput.input():
		shape = line.strip()
		# TODO: verify shape code
		ent = {}
		ent["X"] = num % MAX_X
		ent["Y"] = num // MAX_X
		ent["R"] = 3
		ent["T"] = SIGNAL_TYPE
		ent["C"] = encodeShape(shape)
		data.append(ent)
		num = num + 1
	# pprint(bp)
	jdata = json.dumps(bp, separators=(",", ":")).encode(ENCODING)
	# pprint(jdata)
	return BP_SIG + b64encode(gzip.compress(jdata)).decode(ENCODING) + "$"

def main():
	result = makeConstants()
	print(result)

main()
