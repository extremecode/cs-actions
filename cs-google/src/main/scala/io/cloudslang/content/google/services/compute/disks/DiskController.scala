package io.cloudslang.content.google.services.compute.disks

import com.google.api.services.compute.model._
import io.cloudslang.content.utils.CollectionUtilities.toList

/**
  * Created by victor on 3/5/17.
  */
object DiskController {
  def createDisk(zone: String, diskName: String, sourceImageOpt: Option[String], snapshotImageOpt: Option[String], imageEncryptionKeyOpt: Option[String],
                 diskEncryptionKeyOpt: Option[String], diskType: String, diskDescription: String, licensesList: String,
                 licensesDel: String, diskSize: Long): Disk = {
    val computeDisk = new Disk()
      .setName(diskName)
      .setDescription(diskDescription)
      .setZone(zone)
      .setType(diskType)
      .setLicenses(toList(licensesList, licensesDel))
      .setSizeGb(diskSize)

    if (sourceImageOpt.isDefined) {
      computeDisk.setSourceImage(sourceImageOpt.get)
      imageEncryptionKeyOpt.foreach { encrypt => computeDisk.setSourceImageEncryptionKey(new CustomerEncryptionKey().setRawKey(encrypt)) }
    } else if (snapshotImageOpt.isDefined) {
      computeDisk.setSourceSnapshot(snapshotImageOpt.get)
      imageEncryptionKeyOpt.foreach { encrypt => computeDisk.setSourceSnapshotEncryptionKey(new CustomerEncryptionKey().setRawKey(encrypt)) }
    }
    diskEncryptionKeyOpt.foreach { encrypt => computeDisk.setDiskEncryptionKey(new CustomerEncryptionKey().setRawKey(encrypt)) }
    computeDisk
  }

  def createAttachedDisk(boot:Boolean, autoDelete: Boolean, deviceNameOpt: Option[String], mode: String, source: String, interface: String): AttachedDisk = {
    val attachedDisk = new AttachedDisk()
      .setBoot(boot)
      .setAutoDelete(autoDelete)
      .setMode(mode)
      .setSource(source)
      .setInterface(interface)

    deviceNameOpt match {
      case Some(deviceName) => attachedDisk.setDeviceName(deviceName)
      case _ => attachedDisk
    }
  }

  def createAttachedDisk(boot: Boolean,
                         mountType: String,
                         mountMode: String,
                         autoDelete: Boolean,
                         diskDeviceNameOpt: Option[String],
                         diskName: String,
                         diskSourceImage: String,
                         diskTypeOpt: Option[String],
                         diskSize: Long): AttachedDisk = {
    val attachedDisk = new AttachedDisk()
      .setBoot(boot)
      .setType(mountType)
      .setMode(mountMode)
      .setAutoDelete(autoDelete)
      .setInitializeParams(createAttachedDiskInitializeParams(diskName, diskSourceImage, diskTypeOpt, diskSize))

    diskDeviceNameOpt match {
      case Some(diskDeviceName) => attachedDisk.setDeviceName(diskDeviceName)
      case _ => attachedDisk
    }
  }

  private def createAttachedDiskInitializeParams(diskName: String, diskSourceImage: String, diskTypeOpt: Option[String], diskSize: Long): AttachedDiskInitializeParams = {
    val initParam = new AttachedDiskInitializeParams()
      .setDiskName(diskName)
      .setSourceImage(diskSourceImage)
      .setDiskSizeGb(diskSize)

    diskTypeOpt match {
      case Some(diskType) => initParam.setDiskType(diskType)
      case _ => initParam
    }
  }
}
