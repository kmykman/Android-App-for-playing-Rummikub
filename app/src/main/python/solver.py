import numpy as np
from group import group
import combGen
from itertools import chain, combinations
from collections import Counter
import random

def checkLen(elem):
    """
    (group) -> int

    returns the length of a group
    """
    return len(elem.group)

def solver(currHand, currBoard):
    """
    ([tile], [group]) -> ([group], [tile])

    Takes in the current hand and current board then creates a 
    exhaustive list of all the possibilities for a group in rummikub.
    Finds the possible groups that can be made with the current hand 
    and board. Then checks all possible combinations of possible groups
    to find the set of groups that contains the most tiles
    """
    boardCopy = []
    for item in currBoard:
        boardCopy.extend(item.group)

    exHand = currHand + boardCopy
    exHandStrings = [x.string for x in exHand]
    # print(exHandStrings)

    # this is a list of every possible unique group
    # xarray is the location of all groups that can be made from the tiles in the masterList
    # called_com_gen = False
    # if not called_com_gen:
    exhaustiveList = combGen.generateSet(8,['R','B','K'])
        # called_com_gen = True
    xarray = np.zeros(len(exhaustiveList))
    for item in exhaustiveList:
        jokerCount = 0
        tileCounter = 0
        for el in item.group:
            if el.string in exHandStrings:
                tileCounter += 1
                if el.string == 'J0':
                    exHandStrings.remove('J0')
                    jokerCount += 1
        if tileCounter == len(item.group):
            xarray[exhaustiveList.index(item)] += 1
        exHandStrings.extend(jokerCount*['J0'])
    print(len(exhaustiveList))

    # makeablegroups is a list with all of the groups that can be made from the masterList
    # the list has duplicates of everything to account for the presence of 2 of each tile in the game
    groupsFromHand = np.where(xarray==1)
    allmakeableGroups = 2*list(np.array(exhaustiveList)[groupsFromHand])
    allmakeableGroups = list(set(allmakeableGroups))
    # makeableGroups.sort(key=checkLen,reverse=True)
    allmakeableGroups.sort(key=checkLen)
    print(len(allmakeableGroups))
    
    # print(makeableGroups)
    make = []
    for item in allmakeableGroups:
        make.append(item.group)
    # print(make)
    makeString = []
    for item in allmakeableGroups:
        tmp = []
        for i in item.group:
            tmp.append(i.string)
        makeString.append(tmp)
    print(makeString)

    # this algorithm goes through each group and the following groups and determines the best
    # set of groups to play to maximize the total number of tiles played
    bestPlay = []
    bestLen = 0
    bestChoice = []
    
    boardLen = []
    for item in currBoard:
        boardLen.extend(item.group)
    boardLenString = [x.string for x in boardLen]
    # print("2")
    # subset = list(powerset(makeString, len(exHandStrings), len(boardLenString)))
    # print(subset)
    # print("1")
    # subset = subset[::-1]
    # subset = sorted(subset, key=lambda x: (len(x.group)))
    # subset.sort(key=checkLen,reverse=True)
    round = 0
    # subset = subset.reverse()
    # print(subset[0])
    # print(len(subset))
    # print(len(list(set(subset))))
    
    print("3")
    start = 1
    if len(boardLenString) >= 9 and len(boardLenString) < 17:
        start = 2
    elif len(boardLenString) >= 17 and len(boardLenString) < 25:
        start = 3
    elif len(boardLenString) >= 25 and len(boardLenString) < 33:
        start = 4
    elif len(boardLenString) >= 33 and len(boardLenString) < 41:
        start = 5
    elif len(boardLenString) >= 41:
        start = 6

    print("start")

    
    loop = 1
    if len(allmakeableGroups) > 23:
        loop = 5
    for i in range(loop):
        if len(allmakeableGroups) > 23:
            random.shuffle(allmakeableGroups)
            print(allmakeableGroups)
            print(len(allmakeableGroups))

            # makeableGroups = makeableGroups[:23]
            # print(len(makeableGroups))
            if len(allmakeableGroups) > 50:
                tmpmakeableGroups = np.array_split(allmakeableGroups, 3)
            else:
                tmpmakeableGroups = np.array_split(allmakeableGroups, 2)
            print(len(tmpmakeableGroups))
        else:
            tmpmakeableGroups = [allmakeableGroups]
        print(tmpmakeableGroups)
        for makeableGroups in tmpmakeableGroups:
        
        
            for r in range(start, min(len(makeableGroups)+1, int(len(exHandStrings) // 3) + 1)):
                tmpCount = 0
                # print(r)
                end = len(makeableGroups) + 1
                if r > 1:
                    for loop in range(r - 1):
                        # print(loop)
                        tmpCount += len(makeableGroups[loop].group)
                    # print("new")
                    # print(tmpCount)
                    
                    for loop in range(r-1, len(makeableGroups), -1):
                        # print(loop)
                        if tmpCount + len(makeableGroups[loop].group) > len(exHandStrings):
                            # print(tmpCount)
                            end = loop
                            break
                    # print(end)



                for i in combinations(makeableGroups, r):
                    subsetStrings = 0
                    # print(subset[i])
                    for t in i:
                        for x in t.group:
                            subsetStrings += 1
                    
                    if len(exHandStrings) < subsetStrings:
                        # print(subsetStrings)
                        continue

                    if len(boardLenString) > subsetStrings or bestLen >= subsetStrings:
                        continue
                    
                    round += 1
                    
                    # if (round == 100):
                    #     print("it is 100")
                    #     print(subset[i])
                    # print(boardCopyStrings)
                    # print(subsetStrings)

                    boardCopy = []
                    for item in currBoard:
                        boardCopy.extend(item.group)
                    boardCopyStrings = [x.string for x in boardCopy]
                    
                    masterList = exHand[:]
                    masterStrings = [x.string for x in masterList]

                    useThese = []
                    impossible = False
                    for item in i:
                        tempGroup = []
                        # print(i)
                        # print(item)
                        for el in item.group:
                            # print(el)
                            try:
                                # print(masterList)
                                masterList.pop(masterStrings.index(el.string))
                                masterStrings.remove(el.string)
                            except:
                                masterList.extend(tempGroup)
                                masterStrings.extend([x.string for x in tempGroup])
                                tempGroup = []
                                impossible = True
                                break
                            tempGroup.append(el)
                        # print(tempGroup)
                        if impossible:
                            break

                        if tempGroup != []:
                            useThese.append(tempGroup)
                            # print(useThese)
                    # print("useeeeeeeeee")
                    # print(useThese)
                    
                    # if (len(useThese) > 0):
                    #     newsThese = useThese[0]
                    # else:
                    #     continue
                    if impossible:
                        continue
                    newsThese = []
                    for it in useThese:
                        newsThese.extend(it)
                    # print(newsThese)
                    # print(useThese)
                    # print("1")
                    useTheseStrings = [x.string for x in newsThese]
                    boardCheck = True
                    for el in boardCopyStrings:
                        # print(el)
                        if el in useTheseStrings:
                            useTheseStrings.remove(el)
                        else:
                            boardCheck = False
                            break
                    # if boardCheck:
                    #     print("useeeeeeeeee")
                    #     print(useThese)
                    
                    if boardCheck:
                        bestChoice = useThese[:]
                        bestLen = len(newsThese)
                        bestPlay = masterList[:]
                        # print(bestChoice)
                        # print(list(subset[i]))
                        # print(subsetStrings)
                        # print(len(boardCopyStrings))
                        # print(len(exHandStrings))
    print("round")
    print(round)
    # converts list of lists to list of groups
    groupsToAdd = []
    list_group = []
    for currGroup in bestChoice:
        print(type(currGroup))
        for i in currGroup:
            print(type(i))
        groupsToAdd.append(group(currGroup))
        list_group.append(currGroup)
    
#     if len(groupsToAdd) == 0:
#         return None, None, bestPlay

    grp = []
    new = []
#         print("list_group")
#         print(list_group)
    for i in list_group:
#             print(type(i))
        tmp = []
        for j in i:
#                 print(j)
            tmp.append(str(j))
#                 print("tmp")
#                 print(tmp)
        grp.append(tmp)
    for i in bestPlay:
        new.append(str(i))

    return grp, new
    # return None, bestPlay

def powerset(iterable, total, board):
    arr = list(iterable)
    # arr = [[1], [1,2], [2,3], [3,4], [1,2,3], [1,3,4]]
    # cha = [c for r in range(1,len(arr)+1) for c in combinations(arr, r) if sum(map(len,c)) < 7]
    cha = subsets(arr)
    # print(*ps,sep='\n')
    # s = list(iterable)
    # cha = []
    # print("s")
    # print(s)
    # print(len(s)+1)
    # start = 1
    # if board >= 9 and board < 17:
    #     start = 2
    # elif board >= 17 and board < 25:
    #     start = 3
    # elif board >= 25 and board < 33:
    #     start = 4
    # elif board >= 33 and board < 41:
    #     start = 5
    # elif board >= 41:
    #     start = 6
    # # for r in range(1, len(s)+1):
    # # oi=0
    # cha = []
    
    
    # for r in range(start, min(len(s)+1, int(total // 3) + 1)):
        # print(r)
        # combos = list(filter(lambda e: skip(e, total) , combinations(s, r)))
        # print(list(combos))
        # cha.extend(combos)
        # cha.append(list(combinations(list(range(len(s))),r)))
        # combinations(s, r)


        
        
        # for index in combinations(list(range(len(s))),r):
        #     print(index)
        #     tmp = []
        #     count = 0
        #     for i in index:
        #         print(i)
        #         count += len(s[i].group)
        #         tmp.append(list(s[i].group))
        #     print(tmp)
        #     print(count)
            # for item in i:
            #     for x in item.group:
            #         count += 1
        #     print(i)
        #     print(count)
        #     # if count < board and count > total:
        #     #     continue
        #     if count < board:
        #         continue
        #     elif count > total:
        #         break
        #     else:
        #         cha.append(i)
            




        # for i in combinations(s, r):
        #     count = 0
        #     # oi +=1
        #     # print(oi)
            # for item in i:
            #     for x in item.group:
            #         count += 1
        #     print(i)
        #     print(count)
        #     # if count < board and count > total:
        #     #     continue
        #     if count < board:
        #         continue
        #     elif count > total:
        #         break
        #     else:
        #         cha.append(i)
    # print(len(list(chain.from_iterable(cha))))
    # print(list(combinations(s, 2)))
    # print(list(chain.from_iterable(combinations(s, 2))))
    # print(list(list(combinations(s, r)) for r in range(1, len(s)+1)))
    print(cha)
    return(cha)
    # return chain.from_iterable(cha)
    
    # return chain.from_iterable(combinations(s, r) for r in range(1, len(s)+1))

def skip(comb, total):
    count = 0
    # print(comb)
    for i in range(len(comb)):
        count += len(comb[i].group)
        # for t in comb[i].group:
        #     print(t)
        #     count += len(t)
    # print(count)
    if count > total:
        return False
    return True

def calcSubset(A, res, subset, index):
    # Add the current subset to the result list
    res.append(subset[:])

    # Generate subsets by recursively including and excluding elements
    for i in range(index, len(A)):
        # if sum(map(len,subset)) + len(A[i]) > 6:
        #     continue
        # tmp=subset.copy()
        # tmp.append(A[i])
        c = Counter(list(chain.from_iterable(subset)))
        
        # print(c)

        if len(list(c.values())) > 0:
            if max(list(c.values())) > 2: continue
        # Include the current element in the subset
        if len(subset) > 16: continue
        if sum(map(len,subset)) + len(A[i]) > 26:continue
        subset.append(A[i])
        #if sum(map(len,subset)) > 3:print(sum(map(len,subset)))
        # print(subset)
        #if len(subset) > 4:continue
        

        # Recursively generate subsets with the current element included
        calcSubset(A, res, subset, i + 1)

        # Exclude the current element from the subset (backtracking)
        subset.pop()


def subsets(A):
    subset = []
    res = []
    index = 8
    calcSubset(A, res, subset, index)
    return res