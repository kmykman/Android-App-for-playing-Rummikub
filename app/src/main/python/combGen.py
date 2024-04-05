from itertools import combinations, permutations, product
from group import group
from tile import tile

def checkListDup(a, b):
    """
    ([tile], [tile]) -> (True, False)

    Checks for Duplicates in the list of tiles
    """
    if len(a) != len(b):
        return False
    for i in range(len(a)):
        if a[i].color != b[i].color or a[i].value != b[i].value:
            return False
    return True

def generateNoJokerSet(highestNum, colors):
    """
    (highestNum, [char]) -> ([group])

    Creates an exhaustive list of combinations possible without Jokers
    """
    noJokerSet = []
    for color in colors:
        for i in range(3, highestNum + 1):
            # noJokerSet.append(group([tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 3):
            #     noJokerSet.append(group([tile(i-3, color, False), tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 4):
            #     noJokerSet.append(group([tile(i-4, color, False), tile(i-3, color, False), tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 5):
            #     for per in list(product([True, False], repeat=6)): 
            #         noJokerSet.append(group([tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 6):
            #     for per in list(product([True, False], repeat=7)): 
            #         noJokerSet.append(group([tile(i-6, color, per[6]), tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 7):
            #     for per in list(product([True, False], repeat=8)): 
            #         noJokerSet.append(group([tile(i-7, color, per[7]), tile(i-6, color, per[6]), tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))

            # two = [[(i-2)*10, i-2],[(i-1)*10, i-1],[i*10, i]]
            # pos = list(product(*two))
            # for item in pos:
            #     noJokerSet.append(group([tile(item[0], color, False), tile(item[1], color, False), tile(item[2], color, False)]))
            # if (i > 3):
            #     three = [[(i-3)*10, i-3],[(i-2)*10, i-2],[(i-1)*10, i-1],[i*10, i]]
            #     pos = list(product(*three))
            #     for item in pos:
            #         noJokerSet.append(group([tile(item[0], color, False), tile(item[1], color, False), tile(item[2], color, False), tile(item[3], color, False)]))
            # if (i > 4):
            #     four = [[(i-4)*10, i-4],[(i-3)*10, i-3],[(i-2)*10, i-2],[(i-1)*10, i-1],[i*10, i]]
            #     pos = list(product(*four))
            #     for item in pos:
            #         noJokerSet.append(group([tile(item[0], color, False), tile(item[1], color, False), tile(item[2], color, False), tile(item[3], color, False), tile(item[4], color, False)]))
            # if (i > 5):
            #     five = [[(i-5)*10, i-5],[(i-4)*10, i-4],[(i-3)*10, i-3],[(i-2)*10, i-2],[(i-1)*10, i-1],[i*10, i]]
            #     pos = list(product(*five))
            #     for item in pos:
            #         noJokerSet.append(group([tile(item[0], color, False), tile(item[1], color, False), tile(item[2], color, False), tile(item[3], color, False), tile(item[4], color, False), tile(item[5], color, False)]))
            # if (i > 6):
            #     six = [[(i-6)*10, i-6],[(i-5)*10, i-5],[(i-4)*10, i-4],[(i-3)*10, i-3],[(i-2)*10, i-2],[(i-1)*10, i-1],[i*10, i]]
            #     pos = list(product(*six))
            #     for item in pos:
            #         noJokerSet.append(group([tile(item[0], color, False), tile(item[1], color, False), tile(item[2], color, False), tile(item[3], color, False), tile(item[4], color, False), tile(item[5], color, False), tile(item[6], color, False)]))
            # if (i > 7):
            #     seven = [[(i-7)*10, i-7],[(i-6)*10, i-6],[(i-5)*10, i-5],[(i-4)*10, i-4],[(i-3)*10, i-3],[(i-2)*10, i-2],[(i-1)*10, i-1],[i*10, i]]
            #     pos = list(product(*seven))
            #     for item in pos:
            #         noJokerSet.append(group([tile(item[0], color, False), tile(item[1], color, False), tile(item[2], color, False), tile(item[3], color, False), tile(item[4], color, False), tile(item[5], color, False), tile(item[6], color, False), tile(item[7], color, False)]))

            
            # noJokerSet.append(group([tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 3):
            #     noJokerSet.append(group([tile(i-3, color, False), tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 4):
            #     noJokerSet.append(group([tile(i-4, color, False), tile(i-3, color, False), tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 5):
            #     noJokerSet.append(group([tile(i-5, color, False), tile(i-4, color, False), tile(i-3, color, False), tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 6):
            #     noJokerSet.append(group([tile(i-6, color, False), tile(i-5, color, False), tile(i-4, color, False), tile(i-3, color, False), tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))
            # if (i > 7):
            #     noJokerSet.append(group([tile(i-7, color, False), tile(i-6, color, False), tile(i-5, color, False), tile(i-4, color, False), tile(i-3, color, False), tile(i-2, color, False), tile(i-1, color, False), tile(i, color, False)]))


            noJokerSet.append(group([tile(i-2, color), tile(i-1, color), tile(i, color)]))
            if (i > 3):
                noJokerSet.append(group([tile(i-3, color), tile(i-2, color), tile(i-1, color), tile(i, color)]))
            if (i > 4):
                noJokerSet.append(group([tile(i-4, color), tile(i-3, color), tile(i-2, color), tile(i-1, color), tile(i, color)]))
            if (i > 5):
                noJokerSet.append(group([tile(i-5, color), tile(i-4, color), tile(i-3, color), tile(i-2, color), tile(i-1, color), tile(i, color)]))
            if (i > 6):
                noJokerSet.append(group([tile(i-6, color), tile(i-5, color), tile(i-4, color), tile(i-3, color), tile(i-2, color), tile(i-1, color), tile(i, color)]))
            if (i > 7):
                noJokerSet.append(group([tile(i-7, color), tile(i-6, color), tile(i-5, color), tile(i-4, color), tile(i-3, color), tile(i-2, color), tile(i-1, color), tile(i, color)]))


            
            # for per in list(product([True, False], repeat=3)): 
            #     noJokerSet.append(group([tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 3):
            #     for per in list(product([True, False], repeat=4)): 
            #         noJokerSet.append(group([tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 4):
            #     for per in list(product([True, False], repeat=5)): 
            #         noJokerSet.append(group([tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 5):
            #     for per in list(product([True, False], repeat=6)): 
            #         noJokerSet.append(group([tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 6):
            #     for per in list(product([True, False], repeat=7)): 
            #         noJokerSet.append(group([tile(i-6, color, per[6]), tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 7):
            #     for per in list(product([True, False], repeat=8)): 
            #         noJokerSet.append(group([tile(i-7, color, per[7]), tile(i-6, color, per[6]), tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
                


           
            # for per in list(product([True, False], repeat=3)): 
            #     noJokerSet.append(group([tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 3):
            #     for per in list(product([True, False], repeat=4)): 
            #         noJokerSet.append(group([tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 4):
            #     for per in list(product([True, False], repeat=5)): 
            #         noJokerSet.append(group([tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 5):
            #     for per in list(product([True, False], repeat=6)): 
            #         noJokerSet.append(group([tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 6):
            #     for per in list(product([True, False], repeat=7)): 
            #         noJokerSet.append(group([tile(i-6, color, per[6]), tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
            # if (i > 7):
            #     for per in list(product([True, False], repeat=8)): 
            #         noJokerSet.append(group([tile(i-7, color, per[7]), tile(i-6, color, per[6]), tile(i-5, color, per[5]), tile(i-4, color, per[4]), tile(i-3, color, per[3]), tile(i-2, color, per[2]), tile(i-1, color, per[1]), tile(i, color, per[0])]))
                    
    colorCombinationsList = []
    for i in range(3, len(colors) + 1):
        colorCombinations = list(combinations(colors, i))
        colorCombinationsList.append(colorCombinations)
    for i in range(1, highestNum + 1):
        for colorList in colorCombinationsList:
            for colorcombinations in colorList:
                # tempGroup = []
                # for per in list(product([True, False], repeat=3)): 
                # for color in colorcombinations:
                    
                #     # for per in list(product([True, False], repeat=3)): 
                #     tempGroup.append(tile(i, color))
                noJokerSet.append(group([tile(i, colorcombinations[0]), tile(i, colorcombinations[1]), tile(i, colorcombinations[2])]))
    
    # print(noJokerSet)
    # if len(noJokerSet) != len(set(noJokerSet)):
    #     print("have dup")
    # else:
    #     print("no dup")
    # print(len(noJokerSet))

    return noJokerSet

def generateOneJokerSet(noJokerSet):
    """
    ([group]) -> ([group])

    Creates an exhaustive list of combinations possible with 1 Joker
    """
    oneJokerSet = []
    for party in noJokerSet:
        for i in range(0, len(party.group)):
            partyCopy = party.group[:]
            partyCopy[i] = tile(0, 'J', False)
            oneJokerSet.append(group(partyCopy))
    return oneJokerSet

def generateTwoJokerSet(oneJokerSet):
    """
    ([group]) -> ([group])

    Creates an exhaustive list of combinations possible with 2 Jokers
    """
    twoJokerSet = []
    for party in oneJokerSet:
        for i in range(0, len(party.group)):
            if party.group[i].color != 'J':
                partyCopy = party.group[:]
                partyCopy[i] = tile(0, 'J', False)
                foundDup = False
                
                if len(twoJokerSet) == 0:
                    twoJokerSet.append(group(partyCopy))
 
                for j in twoJokerSet:
                    if checkListDup(partyCopy, j.group):
                        foundDup = True
                        break

                if not foundDup:
                    twoJokerSet.append(group(partyCopy))

    return twoJokerSet

def generateSet(highestNum, colors):
    """
    (int, [char]) -> ([group])

    Creates an exhaustive list of combinations possible with no, 1, and 2 Jokers
    """

    bigSet = generateNoJokerSet(highestNum, colors)
    # oneJokerSet = generateOneJokerSet(bigSet)
    # bigSet.extend(oneJokerSet)
    # twoJokerSet = generateTwoJokerSet(oneJokerSet)
    # bigSet.extend(twoJokerSet)
    return bigSet
