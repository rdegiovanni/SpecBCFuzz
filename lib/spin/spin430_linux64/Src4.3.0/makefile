# SPIN - Verification Software - Version 4.3.0 - June 2007
#
# Copyright (c) 1989-2004 Lucent Technologies, Bell Labs
# All Rights Reserved. For educational purposes only.
# No guarantee whatsoever is expressed or implied by
# the distribution of this code.
#
# Written by:    Gerard J. Holzmann
# Documentation: http://spinroot.com/
# Bug-reports:   bugs@spinroot.com

# CC=gcc		# or any other ansi compatible c compiler
CC=cc -DNXT 		# -DNXT enables the X operator in LTL
CFLAGS=-ansi -D_POSIX_SOURCE	# on some systems add: -I/usr/include

#	on PC:		add -DPC to CFLAGS above
#	on Solaris:	add -DSOLARIS
#	on HP-UX:	add -Aa
#			and add $(CFLAGS) to the spin.o line: $(CC) $(CFLAGS) -c y.tab.c
#	on __FreeBSD__: omit -D_POSIX_SOURCE
#	also recognized: __FreeBSD__ and __OpenBSD__ and __NetBSD__

YACC=yacc		# on Solaris: /usr/ccs/bin/yacc
YFLAGS=-v -d 		# creates y.output and y.tab.h

SPIN_OS= spin.o spinlex.o sym.o vars.o main.o ps_msc.o \
	mesg.o flow.o sched.o run.o pangen1.o pangen2.o \
	pangen3.o pangen4.o pangen5.o guided.o dstep.o \
	structs.o pc_zpp.o pangen6.o reprosrc.o

TL_OS=	tl_parse.o tl_lex.o tl_main.o tl_trans.o tl_buchi.o \
	tl_mem.o tl_rewrt.o tl_cache.o

spin:	$(SPIN_OS) $(TL_OS)
	$(CC) $(CFLAGS) -o spin $(SPIN_OS) $(TL_OS)

spin.o:	spin.y
	$(YACC) $(YFLAGS) spin.y
	$(CC) -c y?tab.c
	rm -f y?tab.c
	mv y?tab.o spin.o

$(SPIN_OS): spin.h

$(TL_OS): tl.h

main.o pangen2.o ps_msc.o: version.h
pangen1.o: pangen1.h pangen3.h
pangen2.o: pangen2.h pangen4.h pangen5.h

# http://spinroot.com/uno/
uno:	spin.o
	uno *.c
	rm -f spin.o y?tab.?

clean:
	rm -f spin *.o y?tab.[ch] y.output y.debug
	rm -f pan.[chmotb] a.out core *stackdump
