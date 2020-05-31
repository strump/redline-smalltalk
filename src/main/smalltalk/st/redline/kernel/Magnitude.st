"Redline Smalltalk, Copyright (c) James C. Ladd. All rights reserved. See LICENSE in the root of this distribution."

Object subclass: #Magnitude.

!Magnitude methodsFor: 'testing'!

< aMagnitude
	"Answer whether the receiver is less than the argument."

	^self subclassResponsibility !

<= aMagnitude
	"Answer whether the receiver is less than or equal to the argument."

	^(self > aMagnitude) not !

> aMagnitude
	"Answer whether the receiver is greater than the argument."

	^aMagnitude < self !

>= aMagnitude
	"Answer whether the receiver is greater than or equal to the argument."

	^aMagnitude <= self !
!
