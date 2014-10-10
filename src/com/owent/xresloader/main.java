/**
 * 
 */
package com.owent.xresloader;

import com.owent.xresloader.data.dst.DataDstImpl;
import com.owent.xresloader.data.dst.DataDstLua;
import com.owent.xresloader.data.dst.DataDstPb;
import com.owent.xresloader.data.src.DataSrcExcel;
import com.owent.xresloader.data.src.DataSrcImpl;
import com.owent.xresloader.scheme.SchemeConf;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * @author owentou
 *
 */
public class main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int ret = ProgramOptions.getInstance().init(args);
		if (ret < 0)
			System.exit(ret);

        ret = SchemeConf.getInstance().initScheme();
        if (ret < 0)
            System.exit(ret);

        // 读入数据表 & 协议编译
        for(String sn : ProgramOptions.getInstance().dataSourceMetas) {
            // 1. 描述信息
            if (false == SchemeConf.getInstance().getScheme().load_scheme(sn)) {
                System.err.println("[ERROR] convert scheme \"" + sn + "\" failed");
                continue;
            }

            // 2. 数据工作簿
            Class ds_clazz = DataSrcExcel.class;
            DataSrcImpl ds = DataSrcImpl.create(ds_clazz);
            if (null == ds) {
                System.err.println("[ERROR] create data source class \"" + ds_clazz.getName() + "\" failed");
                continue;
            }
            ret = ds.init();
            if (ret < 0) {
                System.err.println("[ERROR] init data source class \"" + ds_clazz.getName() + "\" failed");
                continue;
            }

            // 3. 协议描述文件
            DataDstImpl protoDesc = null;
            switch(ProgramOptions.getInstance().protocol){
                case PROTOBUF:
                    protoDesc = new DataDstPb();
                    break;
                default:
                    System.err.println("[ERROR] protocol type \"" + ProgramOptions.getInstance().protocol.toString() + "\" invalid");
                    break;
            }

            if (null == protoDesc)
                continue;
            if (false == protoDesc.init()) {
                System.err.println("[ERROR] protocol desc \"" + ProgramOptions.getInstance().protocol.toString() + "\" init failed");
                continue;
            }

            // 4. 输出类型
            DataDstImpl outDesc = null;
            switch(ProgramOptions.getInstance().outType){
                case BIN:
                    outDesc = protoDesc;
                    break;
                case LUA:
                    outDesc = new DataDstLua();
                    outDesc = outDesc.init()? outDesc: null;
                    break;
                default:
                    System.err.println("[ERROR] output type \"" + ProgramOptions.getInstance().outType.toString() + "\" invalid");
                    break;
            }
            if (null == outDesc)
                continue;

            try {
                OutputStream fos = new FileOutputStream(SchemeConf.getInstance().getOutputFile(), false);
                byte[] data = outDesc.compile(protoDesc);
                fos.write(data);
            } catch (java.io.IOException e) {
                System.err.println("[ERROR] write data to file \"" + SchemeConf.getInstance().getOutputFile() + "\" failed");
                continue;
            }

            System.out.println("[INFO] convert scheme \"" + sn + "\" success");
        }
	}

}
