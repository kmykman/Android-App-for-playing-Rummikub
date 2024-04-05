class colors:
    colorsDict = {'R': '\033[91m',\
                    'B': '\033[94m',\
                    'Y': '\033[93m',\
                    'K': '\033[92m',\
                    'E': '\033[0m',\
                    'J': '\033[0m'}

class tile():
    def __init__(self, value, color):
        """
        (self, int, char, bool) -> tile 

        Initializer for tile class.
        """
        # self.rawvalue = value
        self.value = value
        self.color = color
        # self.second = second
        self.string = self.stringFormat()
        self.cstring = self.coloredStringFormat()

    def __repr__(self):
        # return f"{self.value}, {self.color}, {self.second}"
        return f"{self.value}, {self.color}"

    def getValue(self):
        if self.rawvalue >= 10:
            return self.rawvalue // 10
        else:
            return self.rawvalue
    
    def stringFormat(self):
        """
        (self) -> string
        
        returns the name of the tile formatted as a string
        """
        # if (self.second == True):
        #     dup = "d"
        # else:
        #     dup = ""
        # return "{}{}{}".format(self.color, self.value, dup)
        return "{}{}".format(self.color, self.value)

    def coloredStringFormat(self):
        """
        (self) -> string
        
        returns the name of the tile formatted as a string with the proper color
        """
        return f"{colors.colorsDict[self.color]}{self.string}{colors.colorsDict['E']}"