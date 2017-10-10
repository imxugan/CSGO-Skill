"""This is used to quickly generate coords.json files for nades.
Simply put this in the Smokes (or similar) directory for the map, and input
the pixel coordinates for each smoke/throw location by entering Transform mode
to get the center points.

This will create the files as you input the coordinates, and you can exit file
mode by pressing "Ctrl+C". You also have the option to choose which coord files
to start with before it begins, and you will be prompted before overwritting
existing files."""

import os

def form(num):
    # An error is here acceptable, and desired
    return str(round(num / 1024.0, 3) * 100)

def writeCoords(pos, sx, sy, tx, ty):
    # pos is the file number to save
    # sx is the X coord of the terminal
    # sy is the Y coord of the terminal
    # tx is the X coord of the throw
    # ty is the Y coord of the throw

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
        print(e)

def takeInput():
    while 1:
        sx = input("sx: ")
        try:
            sx = round(float(sx), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")

    if sx == -1:
        return (-1, -1, -1, -1)

    while 1:
        sy = input("sy: ")
        try:
            sy = round(float(sy), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")

    if sy == -1:
        return (-1, -1, -1, -1)

    while 1:
        tx = input("tx: ")
        try:
            tx = round(float(tx), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")

    if tx == -1:
        return (-1, -1, -1, -1)

    while 1:
        ty = input("ty: ")
        try:
            ty = round(float(ty), 1)
            break
        except ValueError:
            print("Bad input, try again\n\n")

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

    while 1:
        print("\n\nFile "+str(pos))
        sx, sy, tx, ty = takeInput()
        if sx == -1:
            print("\nSkipping File...")
        else:
            writeCoords(pos, sx, sy, tx, ty)
        pos += 1

def runGen():
    print("Preparing to Generate Master coords.json File")

    while 1:
        prefiles = []
        print("Please input all Map Name directories to include, one at a time.\nPress Enter again to continue.")

        while 1:
            prefiles[] = input(": ")
            if prefiles[-1] == "":
                prefiles = prefiles[:-1]
                break

        print("Confirm Directories:")
        for d in prefiles:
            print("  "+d)
        c = input("\n(y/n): ")

        if c == "y":
            break

    files = []

    for d in prefiles:
        try:
            with os.scandir(d) as loc:
                for entry in loc:
                    if entry.is_dir():
                        if entry.name == "Smokes":
                            files[] = entry.path
                        elif entry.name == "Flashes":
                            files[] = entry.path
                        elif entry.name == "Fires":
                            files[] = entry.path
        except OSError as e:
            print("OSError for '" + d + "': " + str(e))

    print("Found these directories to be included:")

    for d in files:
        print("  "+d)

    c = input("\nLooks Good?\n(y/n): ")

    if c != "y":
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
                    if os.path.isfile(entry.path + "\\coords.json") and os.path.getsize(entry.path + "\\coords.json") < 500:
                        coordDict[d][] = entry.path + "\\coords.json"
        print("Found " + str(len(coordDict[d])) + " coord files under 500 bytes")

    print("\nReading Coords:\n")

    #Todo: Load all coord files into memory , then compile and save.


## MAIN ##

helpText = \
"""
This is used to create coords.json files for nade maps. Simply place me in the
related Smoke/ Flash folder and run.

You can select which file number to start with, and you can press Ctrl+C at any
time to exit back to the menu.

Coords will not be saved until all 4 points have been entered.

Input -1 into any of the coordinated to skip the current file number.
----------

\n"""

options = \
"""
1) Create coords.json files
2) Generate master coords.json file

"""

print(helpText)

try:
    while 1:
        print(options)
        while 1:

            c = input(": ")

            try:
                if c == "1":
                    runCreate()
                elif c == "2":
                    runGen()
                else:
                    print("Unknown Selection\n")
            except KeyboardInterrupt:
                print("Exiting...\n\n  Main Menu  \n-------------")
                break

except Exception as e:
    print("Goodbye!\n" + str(e))
    input("Press Enter to Quit")
