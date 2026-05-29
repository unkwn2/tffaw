package com.unkwn2.yandexhud

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable

class ProxyBinderParcelable : Parcelable {
    val binder: IBinder

    constructor(binder: IBinder) {
        this.binder = binder
    }

    private constructor(parcel: Parcel) {
        binder = parcel.readStrongBinder()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeStrongBinder(binder)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ProxyBinderParcelable> {
        override fun createFromParcel(parcel: Parcel): ProxyBinderParcelable = ProxyBinderParcelable(parcel)
        override fun newArray(size: Int): Array<ProxyBinderParcelable?> = arrayOfNulls(size)
    }
}
