from tile import tile
from group import GroupError, InvalidJokerError, RunError, SetError, UniqueColorError, group
import os

def takeSecond(elem):
    """
    ([]) -> [][1]
    
    returns the second element of a list
    """
    return elem[1]

def goingOutSolver(solverHand):
    """
    ([tile]) -> ([group], [tile]) or (None, None)

    Given a list of tiles in a hand this function check to see if there is any possible
    way for the player to use this hand to go out with requires enough sets and
    runs to add up to 30 points
    """
    #converts solverHand from [tile] to [[int, char]]
    currHand = []
    jokerCount = 0
    for currTile in solverHand:
        currHand.append([currTile.value, currTile.color])
        if currTile.value == 0:
            jokerCount += 1
    
    tempOutGroups = []
    for i in range(2):
        #orders currHand by value descending and checks for valid sets not including jokers
        currHand.sort(reverse=True)
        tempList, currHand = setCheck(currHand)
        tempOutGroups.extend(tempList)

        #orders currhand by value descending and then by color then checks for runs 
        #not including jokers
        currHand.sort(reverse=True)
        currHand.sort(key=takeSecond)
        tempList, currHand = runCheck(currHand)
        tempOutGroups.extend(tempList)

    firstPass = True
    for i in range(2):
        #check for sets and runs again with jokers included
        if jokerCount >= 1:
            #sets with jokers
            currHand.sort(reverse=True)
            tempList, currHand, jokerCount = setCheckJoker(currHand, jokerCount, firstPass)
            tempOutGroups.extend(tempList)
            
            #runs with jokers
            if jokerCount >= 1:
                currHand.sort(reverse=True)
                currHand.sort(key=takeSecond)
                tempList, currHand, jokerCount = runCheckJoker(currHand, jokerCount, firstPass)
                tempOutGroups.extend(tempList)
        firstPass = False

    #converts tempOutGroups to format = [group] and determines tiles to remove
    outGroups, tilesToRemove = convertToListOfGroups(tempOutGroups)

    #caculates value of OutGroups
    outGroupsValue = calculateOutGroupsValue(outGroups)



    #check if outGroups is worth 30 points
    if outGroupsValue >= 0:
        print(outGroups)
        groupsToAdd = []
        grp = []
        list_group = []
        string_group = []
        for currGroup in outGroups:
#             print(str(currGroup))
#             for i in currGroup:
#                 print(type(i))
#                 print(i)
            groupsToAdd.append(currGroup)
            string_group.append(str(currGroup))
        for s in string_group:
            tmp = []
#             print(s)
            t = ''
            for i in s:
#                 print(i)
                if i != '[' and i != ']' and i !=' ' and i !=',':
                    t = t + i
                    if i != 'R' and i != 'B' and i != 'K':
                        t = t + ', '
                if i == 'R' or i == 'B' or i == 'K':
                    tmp.append(t)
                    t = ''
#                 else:
#                     t = t + ', '
#             print(tmp)
            grp.append(tmp)
#         print(grp)
        new = []
        for i in tilesToRemove:
            new.append(str(i))
#         new = tilesToRemove

#         print("qweqwe")
#         print(new)

        return grp, new
    else:
#         os.system('cls' if os.name == 'nt' else 'clear')
        print("Not able to go out.")
        return None, None

def setCheck(currHand):
    """
    ([[int, char]]) -> ([group], [[int, char]])

    Checks if given a hand can you create any sets based off the rules of a set in
    rummikub, must all be the same number but all different colors.
    """
    prevTile = 0
    tempGroup = []
    tempOutGroups = []
    addedColors = []
    for currTile in currHand:
        if currTile[0] == prevTile and currTile[1] not in addedColors and len(tempGroup) < 5:
            tempGroup.append(currTile)
            addedColors.append(currTile[1])
            if len(tempGroup) == 4:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
                prevTile = 0
                tempGroup = []
                addedColors = []
            elif len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
        elif currTile[0] == prevTile and currTile[1] in addedColors:
            if len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            continue
        else:
            if len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            prevTile = currTile[0]
            tempGroup = []
            addedColors = []
            tempGroup.append(currTile)
            addedColors.append(currTile[1])

    return tempOutGroups, currHand

def setCheckJoker(currHand, numJokers, firstPass):
    """
    ([[int, char]], int, bool) -> ([group], [[int, char]], int)

    Checks if given a hand and number of jokers can you create any sets based off the rules 
    of a set in rummikub, must all be the same number but all different colors.
    """
    prevTile = 0
    tempGroup = []
    tempOutGroups = []
    addedColors = []
    for currTile in currHand:
        if currTile[0] == prevTile and currTile[1] not in addedColors and len(tempGroup) < 5:
            tempGroup.append(currTile)
            addedColors.append(currTile[1])

        elif currTile[0] == prevTile and currTile[1] in addedColors:
            if len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
                tempGroup = []
                addedColors = []
            continue

        elif currTile[0] != prevTile and len(tempGroup) == 2 and len(tempGroup) < 5 and numJokers > 0 and firstPass:
            tempGroup.append([0, 'J'])
            numJokers -= 1
            if numJokers == 0 and len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
                tempGroup = []
                addedColors = []
            elif numJokers == 1 and len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
                tempGroup = []
                addedColors = []

        elif currTile[0] != prevTile and len(tempGroup) >= 1 and len(tempGroup) < 5 and numJokers > 0 and not firstPass:
            tempGroup.append([0, 'J'])
            numJokers -= 1
            if numJokers >= 0 and len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
                tempGroup = []
                addedColors = []
            elif numJokers == 1 and len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
                tempGroup = []
                addedColors = []

        else:
            if len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            prevTile = currTile[0]
            tempGroup = []
            addedColors = []
            tempGroup.append(currTile)
            addedColors.append(currTile[1])

    return tempOutGroups, currHand, numJokers

def runCheck(currHand):
    """
    ([[int, char]]) -> ([group], [[int, char]])

    Checks if given a hand can you create any runs based off the rules of a run in
    rummikub, must be in increasing order and all the same color.
    """
    #checks for runs
    prevTile = 0
    tempGroup = []
    tempOutGroups =[]
    currColor = ''
    
    for currTile in currHand:
        if currTile[0] == prevTile - 1 and currTile[1] == currColor:
            tempGroup.append(currTile)
            prevTile = currTile[0]
            if len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
        elif currTile[0] == prevTile and currTile[1] == currColor:
            if len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            continue
        else:
            if len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            tempGroup = []
            tempGroup.append(currTile)
            prevTile = currTile[0]
            currColor = currTile[1]
    
    for currGroup in tempOutGroups:
        currGroup.reverse()

    return tempOutGroups, currHand

def runCheckJoker(currHand, numJokers, firstPass):
    """
    ([[int, char]], int, bool) -> ([group], [[int, char]], int)

    Checks if given a hand and number of jokers can you create any runs based off the rules 
    of a run in rummikub, must all be the same color in sequential order.
    """
    prevTile = 0
    tempGroup = []
    tempOutGroups =[]
    currColor = ''
    
    for currTile in currHand:
        if currTile[0] == prevTile - 1 and currTile[1] == currColor:
            tempGroup.append(currTile)
            prevTile = currTile[0]
            if len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)

        elif currTile[0] == prevTile and currTile[1] == currColor:
            if len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            continue

        elif currTile[0] >= prevTile and firstPass:
            tempGroup.append([0, 'J'])
            numJokers -= 1
            if numJokers == 0 and len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            elif numJokers == 1 and len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)

        elif currTile[0] >= prevTile and not firstPass:
            tempGroup.append([0, 'J'])
            numJokers -= 1
            if numJokers >= 0 and len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            elif numJokers == 1 and len(tempGroup) >= 3 and currTile == currHand[-1]:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)

        else:
            if len(tempGroup) >= 3:
                tempOutGroups.append(tempGroup)
                for tileToRemove in tempGroup:
                    currHand.remove(tileToRemove)
            tempGroup = []
            tempGroup.append(currTile)
            prevTile = currTile[0]
            currColor = currTile[1]

    for currGroup in tempOutGroups:
        currGroup.reverse()
    
    return tempOutGroups, currHand, numJokers


def convertToListOfGroups(tempOutGroups):
    """
    ([[[int, char]]]) -> ([group], [tile])

    Converts [[[int, char]]] to [group] and determines which tiles need to be removed
    from the players hand a returns a [tile] of tiles that need to be removed.
    """
    tempGroup = []
    tilesToRemove = []
    outGroups = []
    for currGroup in tempOutGroups:
        for currTile in currGroup:
            tempTile = tile(currTile[0], currTile[1])
            tempGroup.append(tempTile)
            tilesToRemove.append(tempTile)
        try:
            outGroups.append(group(tempGroup))

        except GroupError:
            print('That is not a valid group')

        except SetError:
            print('That is not a valid set')

        except RunError:
            print("That is not a valid run")

        except UniqueColorError:
            print('The tiles in your set do not have unique colors')
        
        except InvalidJokerError:
            print("That is not a valid spot for a joker")

        tempGroup = []
    
    return outGroups, tilesToRemove

def calculateOutGroupsValue(outGroups):
    """
    ([group]) -> int

    Calculates the value of a list of groups based on the rules of rummikub. Numbers are
    worth the same number of points as themselves and jokers are 0 points
    """
    outGroupsValue = 0
    for currGroup in outGroups:
        outGroupsValue += currGroup.getGroupValue()
    
    return outGroupsValue