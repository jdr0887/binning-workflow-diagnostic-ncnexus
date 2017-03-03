package org.renci.canvas.binning.diagnostic.ncnexus.ws;

import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.renci.canvas.binning.core.BinningExecutorService;
import org.renci.canvas.binning.core.diagnostic.DiagnosticBinningJobInfo;
import org.renci.canvas.binning.diagnostic.ncnexus.executor.DiagnosticNCNEXUSTask;
import org.renci.canvas.dao.CANVASDAOBeanService;
import org.renci.canvas.dao.CANVASDAOException;
import org.renci.canvas.dao.clinbin.model.DX;
import org.renci.canvas.dao.clinbin.model.DiagnosticBinningJob;
import org.renci.canvas.dao.clinbin.model.DiagnosticStatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagnosticNCNEXUSServiceImpl implements DiagnosticNCNEXUSService {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticNCNEXUSServiceImpl.class);

    private CANVASDAOBeanService daoBeanService;

    private BinningExecutorService binningExecutorService;

    public DiagnosticNCNEXUSServiceImpl() {
        super();
    }

    @Override
    public Response submit(DiagnosticBinningJobInfo info) {
        logger.debug("ENTERING submit(DiagnosticBinningJobInfo)");
        logger.info(info.toString());
        DiagnosticBinningJob binningJob = new DiagnosticBinningJob();
        try {
            binningJob.setStudy("NCNEXUS");
            binningJob.setGender(info.getGender());
            binningJob.setParticipant(info.getParticipant());
            binningJob.setListVersion(Integer.valueOf(info.getListVersion()));
            binningJob.setStatus(daoBeanService.getDiagnosticStatusTypeDAO().findById("Requested"));
            DX dx = daoBeanService.getDXDAO().findById(Integer.valueOf(info.getDxId()));
            logger.info(dx.toString());
            binningJob.setDx(dx);
            List<DiagnosticBinningJob> foundBinningJobs = daoBeanService.getDiagnosticBinningJobDAO().findByExample(binningJob);
            if (CollectionUtils.isNotEmpty(foundBinningJobs)) {
                binningJob = foundBinningJobs.get(0);
            } else {
                binningJob.setId(daoBeanService.getDiagnosticBinningJobDAO().save(binningJob));
            }
            info.setId(binningJob.getId());
            logger.info(binningJob.toString());

            binningExecutorService.getExecutor().submit(new DiagnosticNCNEXUSTask(binningJob.getId()));

        } catch (CANVASDAOException e) {
            logger.error(e.getMessage(), e);
            return Response.serverError().build();
        }
        return Response.ok(info).build();
    }

    @Override
    public DiagnosticStatusType status(Integer binningJobId) {
        logger.debug("ENTERING status(Integer)");
        try {
            DiagnosticBinningJob foundBinningJob = daoBeanService.getDiagnosticBinningJobDAO().findById(binningJobId);
            logger.info(foundBinningJob.toString());
            return foundBinningJob.getStatus();
        } catch (CANVASDAOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BinningExecutorService getBinningExecutorService() {
        return binningExecutorService;
    }

    public void setBinningExecutorService(BinningExecutorService binningExecutorService) {
        this.binningExecutorService = binningExecutorService;
    }

    public CANVASDAOBeanService getDaoBeanService() {
        return daoBeanService;
    }

    public void setDaoBeanService(CANVASDAOBeanService daoBeanService) {
        this.daoBeanService = daoBeanService;
    }

}
