helpText="""This program will run through all the defined throws in the all.json file.
It will check to make sure the file is syntatically correct, as well as making
sure that all the files match up and exist.

It will throw many different and very descriptive warnings should it happen
across any errors or "possible" errors."""

import os, json, sys, traceback
from json.decoder import JSONDecodeError

# This is a dict of the json object names and what their
# specific folder names would be. This is the same list
# that would be found in the App's code!
consts = {
    "dust2": "Dust 2",
    "inferno": "Inferno",
    "cbble": "Cobblestone",
    "mirage": "Mirage",
    "train": "Train"
}

def checkObj(obj):

    # Caller prints "for object at index '{index}' in '{throw}' throws for map key '{key}'" when False
    if len(obj) != 2:
        print("ERROR: Object has incorrect number of keys. Exactly two are expected ", end="")
        return False
    
    if "end" not in obj.keys():
        print(f"ERROR: No END specified ", end="")
        return False

    if "start" not in obj.keys():
        print(f"ERROR: No START specified ", end="")
        return False

    if len(obj["start"]) < 1:
        print(f"ERROR: START has no data ", end="")
        return False

    return True


def checkFileMatch(start, end, direc):

    # Check if START and END coords match the file given. Returns [] if good, otherwise returns a list of warning(s)

    try:
        file = open(direc + "\\coords.json")
        data = json.loads(file.read())
        file.close()
    except FileNotFoundError as e:
        raise e # Reraise it because this error should've already been caught and wouldn't have run this function
    except JSONDecodeError as e:
        return [f"WARNING: Encountered JSONDecodeError at '{direc}': {str(e)}"]

    warns = []
    if "sx" not in data.keys():
        warns.append(f"WARNING: Missing key 'sx' in '{direc}'.")
    elif type(data["sx"]) not in (int, float):
        warns.append(f"WARNING: Key 'sx' is not a number in '{direc}'.")
    elif data["sx"] < 0 or data["sx"] > 100:
        warns.append(f"WARNING: Key 'sx' is out of range (0-100) in '{direc}'.")

    if "sy" not in data.keys():
        warns.append(f"WARNING: Missing key 'sy' in '{direc}'.")
    elif type(data["sy"]) not in (int, float):
        warns.append(f"WARNING: Key 'sy' is not a number in '{direc}'.")
    elif data["sy"] < 0 or data["sy"] > 100:
        warns.append(f"WARNING: Key 'sy' is out of range (0-100) in '{direc}'.")
        
    if "tx" not in data.keys():
        warns.append(f"WARNING: Missing key 'tx' in '{direc}'.")
    elif type(data["tx"]) not in (int, float):
        warns.append(f"WARNING: Key 'tx' is not a number in '{direc}'.")
    elif data["tx"] < 0 or data["tx"] > 100:
        warns.append(f"WARNING: Key 'tx' is out of range (0-100) in '{direc}'.")
        
    if "ty" not in data.keys():
        warns.append(f"WARNING: Missing key 'ty' in '{direc}'.")
    elif type(data["ty"]) not in (int, float):
        warns.append(f"WARNING: Key 'ty' is not a number in '{direc}'.")
    elif data["ty"] < 0 or data["ty"] > 100:
        warns.append(f"WARNING: Key 'ty' is out of range (0-100) in '{direc}'.")

    if len(warns) == 0:
        # All values are valid, compare them
        sx = data["sx"]
        sy = data["sy"]
        tx = data["tx"]
        ty = data["ty"]

        asx = end[0]
        asy = end[1]
        atx = start[1]
        aty = start[2]

        if sx != asx:
            warns.append(f"WARNING: Throw '{start[0]}' has a mismatch for coord 'sx'. Coord.json has {sx} but all.json has {asx}.")

        if sy != asy:
            warns.append(f"WARNING: Throw '{start[0]}' has a mismatch for coord 'sy'. Coord.json has {sy} but all.json has {asy}.")

        if tx != atx:
            warns.append(f"WARNING: Throw '{start[0]}' has a mismatch for coord 'tx'. Coord.json has {tx} but all.json has {atx}.")

        if ty != aty:
            warns.append(f"WARNING: Throw '{start[0]}' has a mismatch for coord 'ty'. Coord.json has {ty} but all.json has {aty}.")

    return warns


def checkStart(start):

    # Only verify that START is formatted correctly
    # Caller prints "in object at index '{index}' in '{throw}' throws for map key '{key}'" when False

    for i in range(len(start)):
        l = start[i]
        
        if type(l) != list:
            print("ERROR: START contains a mix of arrays and objects, or it contains tertiary throw objects. This is NOT ALLOWED! Found ", end="")
            return False

        if len(l) != 3:
            print(f"ERROR: START of arrays must have arrays of length 3, occured at position '{i}' ", end="")
            return False
        
        if type(l[0]) != int:
            print(f"ERROR: Malformed START array at position '{i}', must begin with an identifier INTEGER, not {type(l[0]).__name__}! Found ", end="")
            return False
        if type(l[1]) not in (int, float):
            print(f"ERROR: Malformed START array at position '{i}', must have a number at index 1, not {type(l[1]).__name__}! Found ", end="")
            return False
        if l[1] < 0 or l[1] > 100:
            print("ERROR: START coord at position 1 is out of range (0-100) ", end="")
            return False
        if type(l[2]) not in (int, float):
            print(f"ERROR: Malformed START array at position '{i}', must have a number at index 2, not {type(l[2]).__name__}! Found ", end="")
            return False
        if l[2] < 0 or l[2] > 100:
            print("ERROR: START coord at position 2 is out of range (0-100) ", end="")
            return False
        
    return True


def checkEnd(end):

    # Only verify that END is formatted correctly
    # Caller prints "for object at index '{index}' in '{throw}' throws for map key '{key}'" when False

    if len(end) != 2:
        print("ERROR: END is not of length 2 ", end="")
        return False
    
    if type(end[0]) not in (int, float):
        print("ERROR: END does not have a number in position 0 ", end="")
        return False
    if end[0] < 0 or end[0] > 100:
        print("ERROR: END coord at position 0 is out of range (0-100) ", end="")
        return False
    
    if type(end[1]) not in (int, float):
        print("ERROR: END does not have a number in position 1 ", end="")
        return False
    if end[1] < 0 or end[1] > 100:
        print("ERROR: END coord at position 1 is out of range (0-100) ", end="")
        return False

    return True


def validateThrows(key, throw, jdata):

    if len(jdata) == 0:
        print(f"ERROR: No '{throw}' throws found for map key '{key}'!")
        return False
    elif len(jdata) <= 5:
        print(f"WARNING: Very low number of unique '{throw}' throws for map key '{key}'!")

    for index in range(len(jdata)):
        obj = jdata[index]

        if not checkObj(obj) :
            print(f"for object at index '{index}' in '{throw}' throws for map key '{key}'")
            return False
        
        if not checkEnd(obj["end"]):
            print(f"for object at index '{index}' in '{throw}' throws for map key '{key}'")
            return False

        if type(obj["start"][0]) == dict:
            if len(obj["start"]) == 1:
                print(f"ERROR: START contains exactly one object. Sad! Found in object at index '{index}' in '{throw}' throws for map key '{key}'")
                return False
            sx = 0.0
            sy = 0.0
            for i in range(len(obj["start"])):
                d = obj["start"][i]
                if type(d) != dict:
                    print(f"ERROR: START contains a mix of arrays and objects. This is NOT allowed! Found in object at index '{index}' in '{throw}' throws for map key '{key}'")
                    return False

                if not checkObj(d):
                    print(f"in position '{i}' of START for object at index '{index}' in '{throw}' throws for map key '{key}'")
                    return False

                if not checkEnd(d["end"]):
                    print(f"at START position '{i}' for object at index '{index}' in '{throw}' throws for map key '{key}'")
                    return False

                if not checkStart(d["start"]):
                    print(f"in position '{i}' of START for object at index '{index}' in '{throw}' throws for map key '{key}'")
                    return False

                warns = []
                for a in range(len(d["start"])):
                    l = d["start"][a]
                    checkdir = consts[key] + "\\" + throw.capitalize() + "\\" + str(l[0])
                    if not os.path.isdir(checkdir):
                        print(f"ERROR: Throw '{l[0]}' is defined in START array at position '{a}', but no such folder exists ({checkdir}). Found in position '{i}' of START for object at index '{index}' in '{throw}' throws for map key '{key}'")
                        return False
                    warns += checkFileMatch(l, d["end"], checkdir) # Assumes everything is correct, returns warnings for format printing

                for warn in warns:
                    print(warn + f"for object at index '{index}' in '{throw}' throws for map key '{key}'")

                sx += d["end"][0]
                sy += d["end"][1]

            if (abs(obj["end"][0] - round(sx / (i + 1), 1)) > 0.001) or (abs(obj["end"][1] - round(sy / (i + 1), 1)) > 0.001):
                print(f"WARNING: Possible average END error, should be ({round(sx / (i + 1), 1)}, {round(sy / (i + 1), 1)}) for object at index '{index}' in '{throw}' throws for map key '{key}'")
            
        elif type(obj["start"][0]) == list:
            if not checkStart(obj["start"]):
                print(f"in object at index '{index}' in '{throw}' throws for map key '{key}'")
                return False
            warns = []
            for l in obj["start"]:
                checkdir = consts[key] + "\\" + throw.capitalize() + "\\" + str(l[0])
                if not os.path.isdir(checkdir):
                    print(f"ERROR: Throw '{l[0]}' is defined in START array, but no such folder exists ({checkdir}). Found in START for object at index '{index}' in '{throw}' throws for map key '{key}'")
                    return False
                warns += checkFileMatch(l, obj["end"], checkdir) # Assumes everything is correct, returns warnings for format printing
            for warn in warns:
                print(warn + f"for object at index '{index}' in '{throw}' throws for map key '{key}'")
                
        else:
            print(f"ERROR: START does not start with an array or object at index '{index}' in '{throw}' throws for map key '{key}'")
            return False

    # All tests passed, this time!
    return True


def runCheck():

    try:
        file = open("all.json")
        data = json.loads(file.read())
        file.close()
    except FileNotFoundError as e:
        print("Unable to open 'all.json'!\n" + str(e))
        return False
    except JSONDecodeError as e:
        print("Bad json format in 'all.json'!\n" + str(e))
        return False
    
    for key in consts.keys():
        print(consts[key])
        if key not in data.keys():
            print(f"WARNING: Map key '{key}' not found in all.json!")
            continue
        else:
            
            if not os.path.isdir(consts[key]):
                print(f"ERROR: Map key '{key}' is defined but the folder '{consts[key]}' was not found!")
                return False

            if "smokes" not in data[key].keys():
                print(f"WARNING: Map key '{key}' does not contain the key 'smokes'!")
            else:
                if not os.path.isdir(consts[key] + "\\Smokes"):
                    print(f"ERROR: Map key '{key}' defines 'smokes' but no folder '{consts[key]}\\Smokes' was found!")
                if not validateThrows(key, "smokes", data[key]["smokes"]):
                    return False
        print()
    return True


print(helpText)

try:
    while 1:
        input("Press ENTER to start test... ")
        if runCheck():
            print("Test passed! Although syntax is correct, there may be warnings.\nDon't forget to minify the json.")
            break
        else:
            print("Failed! Fix the errors and try again.\n")
            
except Exception as e:
    print("Something unexpected happened!")
    print("Traceback (most recent call last):", file=sys.stderr)
    traceback.print_tb(e.__traceback__)
    print(type(e).__name__ + ": " + str(e), file=sys.stderr)
    
finally:
    input("Press ENTER to exit program... ")
