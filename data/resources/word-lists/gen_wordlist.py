import json

# value from 0 - 5
min_frequency = 2
# min and max length for words
min_length = 4
max_length = 11

# function for removing all words that contain non alphabetical chars and convering all to uppercase
def check_chars(word):
    chars = ["a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z","ß","ä","ö","ü"]
    for char in word.lower():
        if not char in chars:
            return False
    return word.upper()

with open('dwds_lemmata_2025-02-09.json', 'r') as file:
    data = json.load(file)

# Head of list file with "lenghts" generated from script parameters
wordlist = "1Q,W,E,R,T,Z,U,I,O,P,Ü\n2A,S,D,F,G,H,J,K,L,Ö,Ä\n3DEL,Y,X,C,V,B,N,M,SEND\n4LENGTHS:"
for i in range(min_length, max_length+1):
    wordlist += str(i) + ","
wordlist = wordlist[:-1]

# put applicable words in list
length = 0
for entry in data:
    if isinstance(entry["freq"], int):
        if int(entry["freq"]) >= min_frequency:
            word =  entry["lemma"]
            if len(word) >= min_length and len(word) <= max_length:
                word = check_chars(word)
                if word:
                    wordlist += "\n" + word
                    length += 1

file = open("wordlist.txt", "w")
file.write(wordlist)
print(length)
