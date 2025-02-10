# Word List Specification

```
1Q,W,E,R,T,Y,U,I,O,P
2A,S,D,F,G,H,J,K,L,DEL
3Z,X,C,V,B,N,M,SEND
4LENGTHS:4,5,6,7,8,9,10,11
AAHED
AAHING
AAHS
```

## Keyboard Spec (Line 1-3)

Prefix number, then the keys separated by a comma.

**Important:** only characters which are listed here can be used to guess words. Therefore check, none of the words in your word list contain those.

The key board will be built up according to this. There must be exactly three rows.

All letters have to be in upper case.

`DEL` and `SEND` are special placeholders and will be replaced accordingly. You cannot do any translations here.

## Available Word lengths (Line 4)

The line has to start with `4LENGTHS:`, then comma separated the available lengths of words. The list provided to the user will be empty if there is no word with a matching length in the list. Note that currently only word lengths from 4 to 11 are possible.

## Word List

Now comes the word list. All words have to be in upper case.

# Licences and attribution

## German word list

The file "dwds_lemmata_2025-02-09.json" is from [dwds](https://www.dwds.de/lemma/list). Its licenced under  the [Creative Commons BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.de) licence.
The file "Deutsch.txt" is generated based on "dwds_lemmata_2025-02-09.json". The same licence applies.
