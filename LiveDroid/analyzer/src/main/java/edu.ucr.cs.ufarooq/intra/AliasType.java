package edu.ucr.cs.ufarooq.intra;

public enum AliasType {
    FieldAlias, //alias for field
    FieldFieldAlias, // alias for member variables of field
    LocalAlias, // create alias for local
    ThisLocal, //this class local, always created
    ParameterLocal,//parameter local alias, created for all params
    ReturnAlias, // alias created return values such as for getter method
    NotDefined,
    InvokeBaseAlias, //special alias, to transfer base variable, $r1.mathodCall()
}