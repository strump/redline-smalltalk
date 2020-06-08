self collect: [:each |
    it := each.
    each == #true ifTrue: [it := true].
          each == #false ifTrue: [it := false].
    each == #nil ifTrue: [it := nil].
    each class == String ifTrue: [
        it := Compiler evaluate: each].
    each class == Array ifTrue: [it := it evalStrings].
    it].
