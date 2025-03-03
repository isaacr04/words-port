# Word List Specification  

```
1Q,W,E,R,T,Y,U,I,O,P  
2A,S,D,F,G,H,J,K,L,DEL  
3Z,X,C,V,B,N,M,SEND  
4LENGTHS:4,5,6,7,8,9,10,11  
AAHED  
AAHING  
AAHS
----^^^^^-secret-^^^^----vvvv-public-vvvv----
ABAKAS
ABALONE
ABALONES
ABAMP
ABAMPERE
ABAMPERES
```

## Keyboard Layout (Lines 1-3)  

Each line starts with a **row number**, followed by a comma-separated list of keys.  

- **Only the listed characters** can be used to guess words. Ensure that no words in your list contain **other characters**.  
- The keyboard consists of **exactly three rows**.  
- **All letters must be uppercase**.  
- `DEL` and `SEND` are **special placeholders**. These will be replaced accordingly and **cannot be modified**.  

## Word Lengths (Line 4)  

This line must start with `4LENGTHS:`, followed by a comma-separated list of **allowed word lengths**.  

- Only words with the specified lengths will be available to users.  
- If no words match a given length, the list will appear **empty**.  
- Supported word lengths: **4 to 11 characters**.  

## Word List (Starting from Line 5)  

The word list follows containing two sections, the secret words and the none secret words.

- **All words must be uppercase**.  
- Words must only contain characters from the **Keyboard Layout** section.  

### Secret Words

The first part of the wordlist are the secret words.
Those words are the pool for selecting the secret word, but are also part of the allowed list, the user can pick from for guessing.
When there are no matching secret words with the correct wordlength, the program will fallback to the allowed word list and pick from there.

### Separator

The separator has to start with '-' and separates the section of the secret words to the general allowed words.

### Non Secret Words

Now follows words from which the user can pick from for guessing, but which will never show up as a secret word.

# Licences and attribution

## English word list

Source [Wordnik](https://www.wordnik.com/)

> You are free to use this list in any way you'd like. This includes
> commercial uses, though I'd appreciate it if you didn't just turn
> around and try to sell it (but I mean, I'll still offer it for free
> to anyone so that wouldn't be a smart business venture anyway).

The wordnik list is available under the terms of the MIT license, and its license file is included unmodified in this directory in
`LICENSE.wordnik`


## German word list

The file "dwds_lemmata_2025-02-09.json" is from [dwds](https://www.dwds.de/lemma/list). Its licenced under  the [Creative Commons BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.de) licence.
The file "Deutsch.txt" is generated based on "dwds_lemmata_2025-02-09.json". The same licence applies.
