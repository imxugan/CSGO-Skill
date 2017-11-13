"""This is used to quickly generate coords.json files for nades.
Simply put this in the Smokes (or similar) directory for the map, and input
the pixel coordinates for each smoke/throw location by entering Transform mode
to get the center points.

Using Photoshop, press CTRL+T to enter Transform mode, and look in the upper
left of the window to find the X and Y coordinates.

This assumes the image is 1024x1024 pixels. If it isn't, blame whoever made the
image and fix it yourself!

This will create the files as you input the coordinates, and you can exit file
mode by pressing "Ctrl+C". You also have the option to choose which coord files
to start with before it begins, and you will be prompted before overwritting
existing files."""

import os, re, json, logging

logging.basicConfig(filename=".log", level=logging.DEBUG)

def form(num):
    # An error is here acceptable, and desired
    return str(round(num / 1024.0, 3) * 100)

def atoi(text):
    return int(text) if text.isdigit() else text

def natural_keys(text):
    '''
    alist.sort(key=natural_keys) sorts in human order
    http://nedbatchelder.com/blog/200712/human_sorting.html
    '''
    return [ atoi(c) for c in re.split('(\d+)', text) ]

def writeCoords(pos, sx, sy, tx, ty):
    # pos is the file number to save
    # sx is the X coord of the terminal
    # sy is the Y coord of the terminal
    # tx is the X coord of the start
    # ty is the Y coord of the start
    # I know this is backwards just trust me

    try:
        f = open(str(pos)+"/coords.json", "w")
        string = '{\r\n'+\
                 '    "sx":'+form(sx)+',\r\n'+\
                 '    "sy":'+form(sy)+',\r\n'+\
                 '    "tx":'+form(tx)+',\r\n'+\
                 '    "ty":'+form(ty)+'\r\n}'
        f.write(string)
        f.close()
    except Exception as e:
        logging.exception("Got exception in writeCoords()")
        raise

def takeInput():
    while 1:
        sx = input("sx: ")
        try:
            sx = round(float(sx), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")
        except:
            logging.exception("Got exception in takeInput()")
            raise

    if sx == -1:
        return (-1, -1, -1, -1)

    while 1:
        sy = input("sy: ")
        try:
            sy = round(float(sy), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")
        except:
            logging.exception("Got exception in takeInput()")
            raise

    if sy == -1:
        return (-1, -1, -1, -1)

    while 1:
        tx = input("tx: ")
        try:
            tx = round(float(tx), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")
        except:
            logging.exception("Got exception in takeInput()")
            raise

    if tx == -1:
        return (-1, -1, -1, -1)

    while 1:
        ty = input("ty: ")
        try:
            ty = round(float(ty), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")
        except:
            logging.exception("Got exception in takeInput()")
            raise

    if ty == -1:
        return (-1, -1, -1, -1)

    return (sx, sy, tx, ty)

def runCreate():

    while 1:
        pos = input("Input Start File Number\n: ")
        try:
            pos = int(round(float(pos), 0))
            if pos > 0:
                break
            print("Must be a positive integer!\n\n")
        except ValueError:
            print("Bad input, try again\n\n")
        except:
            logging.exception("Got exception in runCreate()")
            raise

    while 1:
        print("\n\nFile "+str(pos))
        sx, sy, tx, ty = takeInput()
        if sx == -1:
            print("\nSkipping File...")
        else:
            writeCoords(pos, sx, sy, tx, ty)
        pos += 1


_rem = """ This function has been removed because it may be faster to manually
           create our own master json file, with more accuracy, than by writing
           a function for it.
def runGen():
    print("Preparing to Generate Master coords.json File")

    while 1:
        prefiles = []
        print("Please input all Map Name directories to include, one at a time.\nPress Enter again to continue.")

        while 1:
            prefiles.append(input(": "))
            if prefiles[-1] == "":
                prefiles = prefiles[:-1]
                break

        print("Confirm Directories:")
        for d in prefiles:
            print("  "+d)
        c = input("\n(y/n): ").lower()

        if c == "y" or c == "":
            break

    files = []

    for d in prefiles:
        try:
            with os.scandir(d) as loc:
                for entry in loc:
                    if entry.is_dir():
                        if entry.name == "Smokes":
                            files.append(entry.path)
                        elif entry.name == "Flashes":
                            files.append(entry.path)
                        elif entry.name == "Fires":
                            files.append(entry.path)
        except OSError as e:
            print("OSError for '" + d + "': " + str(e))
            logging.exception("Got exception in runGen()")


    print("Found these directories to be included:")

    for d in files:
        print("  "+d)

    c = input("\nLooks Good?\n(y/n): ").lower()

    if c != "y" and c != "":
        print("Exiting...")
        return

    print("\nDiscovering Coords:\n")

    coordDict = {}

    for d in files:
        print("  "+d+" .. ", end="")
        coordDict[d] = []
        with os.scandir(d) as loc:
            for entry in loc:
                if entry.is_dir() and entry.name.isdecimal() and os.path.exists(entry.path + "\\coords.json"):
                    if os.path.isfile(entry.path + "\\coords.json") and os.path.getsize(entry.path + "\\coords.json") < 100:
                        coordDict[d].append(entry.path + "\\coords.json")
        coordDict[d].sort(key=natural_keys)
        print(coordDict[d])
        print("Found " + str(len(coordDict[d])) + " coord files under 100 bytes")

    print("\nSettings - Press enter to accept default setting\n")

    while 1:

        while 1:
            compress = -1
            print(\
            "Compression Settings:\n"+\
            "  None:   Only exact matches\n"+\
            "  Low:    Within 1 Percent (Default)\n"+\
            "  Medium: Within 2 Percent\n"+\
            "  High:   Within 3 Percent\n")
            c = input(": ").lower()

            if c == "none":
                c = "NO"
                compress = 0
            elif c == "low" or c == "":
                c = "LOW"
                compress = 1
            elif c == "medium":
                c = "MEDIUM"
                compress = 2
            elif c == "high":
                c = "HIGH"
                compress = 3
            else:
                print("Unknown Selection\n")

            if not compress == -1:
                break

        while 1:
            aggregate = -1
            print(\
            "Aggregation Settings:\n"+\
            "  Low:    Within  5 Percent\n"+\
            "  Medium: Within  8 Percent (Default)\n"+\
            "  High:   Within 10 Percent\n")
            a = input(": ").lower()

            if a == "low":
                a = "LOW"
                aggregate = 5
            elif a == "medium" or a == "":
                a = "MEDIUM"
                aggregate = 8
            elif a == "high":
                a = "HIGH"
                aggregate = 10
            else:
                print("Unknown Selection\n")

            if not aggregate == -1:
                break

        print(\
        "Confirm Settings:\n"+\
        "  " + c + " compression\n"+\
        "  " + a + " aggregation\n")

        c = input("(y/n): ").lower()
        if c == "y" or c == "":
            break

    print("\nReading Coords:\n")

    # Load all coord files into mem object
    mem = {}
    for mapname in list(coordDict.keys()):
        mem[mapname] = {"Smokes":[],"Flashes":[],"Fires":[]}
        print(mapname + "(" + str(len(coordDict[mapname])) + ")", end="")
        cur = ""
        for loc in coordDict[mapname]:

            if cur != "Smokes" and "Smokes" in loc:
                cur = "Smokes"
                print("\n  Smokes", end="")
            elif cur != "Flashes" and "Flashes" in loc:
                cur = "Flashes"
                print("\n  Flashes", end="")
            elif cur != "Fires" and "Fires" in loc:
                cur = "Fires"
                print("\n  Fires", end="")

            f = open(loc)
            f = f.read()
            mem[mapname][cur].append(json.loads(f))

            # Check for valid format
            chkeys = list(mem[mapname][cur][-1].keys())
            if "sx" not in chkeys:
                raise SyntaxError("Key \"sx\" not found for path \"" + loc + "\"")
            if "sy" not in chkeys:
                raise SyntaxError("Key \"sy\" not found for path \"" + loc + "\"")
            if "tx" not in chkeys:
                raise SyntaxError("Key \"tx\" not found for path \"" + loc + "\"")
            if "ty" not in chkeys:
                raise SyntaxError("Key \"ty\" not found for path \"" + loc + "\"")

            print(".", end="")
        print()

    #Todo: Compile and save.

    print("\nGenerating File\n")

    file_contents = "{"

    for mapname in list(mem.keys()):
        file_contents += '"' + mapname + '":{'

        # Smokes
        file_contents += '"smokes":['

        # Build a list of all throws with indexes
        ends = []
        i = 0
        for obj in mem[mapname]["Smokes"]:
            ends.append([i, [obj["sx"], obj["sy"]]])
            i += 1

        # Compare current throw to all following throws, compressing
        for obj in ends:
            last = obj
            for obj2 in ends[obj[0] + 1:]:
                if abs(obj[-1][0] - obj2[-1][0]) <= compress and abs(obj[1][1] - obj2[1][1]) <= compress:
                    temp = []
                    temp += ends[obj[0]][0:-1] + [obj2[0]]
                            # [0, 1]
                    temp.append(obj[-1])
                            # [0, 1, [x, y]]
                    ends[ends.index(last)] = temp
                    ends.remove(obj2)
                    last = temp

        #print(ends)

        # Compare current throw to all following throws, aggregating
        final = []
        i = 0
        for obj in ends:
            i += 1
            for obj2 in ends[i:]:

"""


## MAIN ##

helpText = \
"""
Welcome to Coordinate Maker! This will make generating coordinate files EZ!

"""

createText = \
"""
This is used to create coords.json files for nade maps. Place me in the related
Smoke/ Flash/ Fire folder if you haven't already.

You can select which file number to start with, and you can press Ctrl+C at any
time to exit back to the menu.

There are 4 numbers for each coords.json file:
    sx: the X coord of the terminal as a pixel value
    sy: is the Y coord of the terminal
    tx is the X coord of the throw
    ty is the Y coord of the throw
Coords will not be saved until all 4 points have been entered.

Input -1 into any of the coordinated to skip the current file number.
----------

"""

options = \
"""
1) Create coords.json files
2) Generate master coords.json file

"""

print(helpText)

try:
    while 1:

        print(createText)
        runCreate()

###        print(options)
###        while 1:
###
###            c = input(": ")
###
###            try:
###                if c == "1":
###                    print(createText)
###                    runCreate()
###                elif c == "2":
###                    runGen()
###                else:
###                    print("Unknown Selection\n")
###            except KeyboardInterrupt:
###                print("Exiting...\n\n  Main Menu  \n-------------")
###                break

except Exception as e:
    print("\n\nGoodbye!\n")
    logging.exception("Got exception in Main")
    input("Press Enter to Quit")
