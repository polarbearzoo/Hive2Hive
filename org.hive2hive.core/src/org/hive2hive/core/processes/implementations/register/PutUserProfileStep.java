package org.hive2hive.core.processes.implementations.register;

import java.io.IOException;

import javax.crypto.SecretKey;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.hive2hive.core.H2HConstants;
import org.hive2hive.core.exceptions.PutFailedException;
import org.hive2hive.core.model.UserProfile;
import org.hive2hive.core.network.data.IDataManager;
import org.hive2hive.core.processes.framework.exceptions.InvalidProcessStateException;
import org.hive2hive.core.processes.framework.exceptions.ProcessExecutionException;
import org.hive2hive.core.processes.implementations.common.base.BasePutProcessStep;
import org.hive2hive.core.security.EncryptedNetworkContent;
import org.hive2hive.core.security.H2HEncryptionUtil;
import org.hive2hive.core.security.PasswordUtil;
import org.hive2hive.core.security.UserCredentials;

public class PutUserProfileStep extends BasePutProcessStep {

	private final UserCredentials credentials;
	private final UserProfile userProfile;

	public PutUserProfileStep(UserCredentials credentials, UserProfile userProfile, IDataManager dataManager) {
		super(dataManager);
		this.credentials = credentials;
		this.userProfile = userProfile;
	}

	@Override
	protected void doExecute() throws InvalidProcessStateException, ProcessExecutionException {
		// encrypt user profile
		SecretKey encryptionKey = PasswordUtil.generateAESKeyFromPassword(credentials.getPassword(),
				credentials.getPin(), H2HConstants.KEYLENGTH_USER_PROFILE);

		EncryptedNetworkContent encryptedProfile = null;
		try {
			encryptedProfile = H2HEncryptionUtil.encryptAES(userProfile, encryptionKey);
		} catch (DataLengthException | IllegalStateException | InvalidCipherTextException | IOException e) {
			throw new ProcessExecutionException("User profile could not be encrypted.");
		}

		try {
			encryptedProfile.generateVersionKey();
		} catch (IOException e) {
			throw new ProcessExecutionException("User profile version key could not be generated.", e);
		}

		// put encrypted user profile
		try {
			put(credentials.getProfileLocationKey(), H2HConstants.USER_PROFILE, encryptedProfile,
					userProfile.getProtectionKeys());
		} catch (PutFailedException e) {
			throw new ProcessExecutionException(e);
		}

	}
}