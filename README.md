# open.term #

> pronounced open DOT term

### What is this repository for? ###

Am open source emulator supporting 3270 and potentially later 5250 terminal types. The name, is derived from  Ascert's package root for open source contributions. This being a terminal emulator, it's packages naturally have a .term suffix - hence the full name of "open.term"

The original purpose of this package was for code control of a host terminal. Over time if has become a useful, if basic, terminal emulator in it's own right.

The simplest way is direct from gradle:

```
gradlew execute
```
There are various properties you can use for a more enhanced experience e.g. the following will pre-populate the list of 
available hosts, skipping the initial dialog box:

```
gradlew -Dcom.ascert.open.term.available=127.0.0.1,2023,false,IBM-3278-2-E;10.1.2.3,23,false,IBM-32792-E execute
```

### Origins ###

The original code was based on [Freehost3270](https://github.com/AlanKrueger/freehost3270), and also the [IETF OHIO](https://tools.ietf.org/html/draft-ietf-tn3270e-ohio-01) API spec. Both of these being rather old and unmaintained, a lot has changed though hence the new naming and packaging. Licensing is based on LGPL to respect the original works.

### OHIO ###

An original org.ohio package was used as a starting point, but modified and tidied (somewhat) into the current com.ascert.open.ohio classes. It's a little debatable whether the OHIO model has much merit given it's lack of widespread use and adoption. As a result of this, a more specific direct API may be created in place of the OHIO model. 

### What about NonStop 6530 support ###

I'm glad you asked about that. 

Ascert LLC offers a commercial NonStop 6530 datastream handling library and terminal emulator called **term.6530**. It is of course based around the open.term core which we maintain here, but with additions and extensions for handling NonStop 6530 terminals. Feel free to email Ascert for more details: info@ascert.com

### Why wasn't the first release 1.0.0? ###

Basically, the original Freehost3270 code seemed to have a 0.2.0 version marker. Ascert moved to a 1.0 version to reflect the significant amount of refactoring. It took a couple of internal releases to get this all done to a reasonably decent level and also working on GitHub and JitPack. 

### Code Style ###

The include [NetBeans Formatting export](NetBeansJavaStyle.zip) can be used for ease of conforming to Java coding style.

