import json
import gzip
import fileinput

from base64 import b64decode, b64encode
from pprint import pprint

ENCODING = "utf-8"
BP_SIG = "SHAPEZ2-3-"

SIGNAL_TYPE = "ConstantSignalDefaultInternalVariant"

EMPTY_BP = """{
	"V": 1122,
	"BP": {
  	"$type": "Building",
  	"Icon": {"Data": ["icon:Buildings", null, null, "shape:CuCuCuCu"]},
  	"Entries": [],
  	"BinaryVersion": 1122
	}
}"""

def encodeValue(value):
	if (value.isdigit()):
		num = int(value)
		v = "\x03" + chr(num & 0xff) + chr((num >> 8) & 0xff) + "\x00\x00"
	else:
		# TODO: verify shape code
		v = "\x06\x01\x01" + chr(len(value)) + "\x00"  + value
	pprint(v)
	return b64encode(v.encode(ENCODING)).decode(ENCODING)

MAX_X = 16

def makeConstants():
	bp = json.loads(EMPTY_BP)
	data = bp["BP"]["Entries"]
	num = 0
	for line in fileinput.input():
		value = line.strip()
		ent = {}
		ent["X"] = num % MAX_X
		ent["Y"] = num // MAX_X
		ent["R"] = 3
		ent["T"] = SIGNAL_TYPE
		ent["C"] = encodeValue(value)
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
